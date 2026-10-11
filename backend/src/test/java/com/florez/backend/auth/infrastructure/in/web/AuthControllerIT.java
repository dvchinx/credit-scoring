package com.florez.backend.auth.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuthControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_conCredencialesDelAdminBootstrap_devuelveToken() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequestPayload("admin", "ChangeMe123!"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("token"))
                .andExpect(result -> {
                    var json = objectMapper.readTree(result.getResponse().getContentAsString());
                    assertThat(json.get("username").asText()).isEqualTo("admin");
                    assertThat(json.get("role").asText()).isEqualTo("ADMIN");
                });
    }

    @Test
    void preflightCors_desdeElFrontendConfigurado_seAutoriza() throws Exception {
        mockMvc.perform(options("/credit-applications")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        .isEqualTo("http://localhost:5173"));
    }

    @Test
    void preflightCors_desdeUnOrigenNoConfigurado_seRechaza() throws Exception {
        mockMvc.perform(options("/credit-applications")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(403))
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                        .isNull());
    }

    @Test
    void login_conCredencialesInvalidas_devuelve401() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequestPayload("admin", "wrong-password"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    @Test
    void createUser_sinAutenticacion_devuelve401() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateUserPayload("someone", "secret123", "ANALYST"));

        mockMvc.perform(post("/auth/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    @Test
    void createUser_comoAnalista_devuelve403YNo500() throws Exception {
        String adminToken = login("admin", "ChangeMe123!");
        mockMvc.perform(post("/auth/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateUserPayload("analyst-403", "secret123", "ANALYST"))));
        String analystToken = login("analyst-403", "secret123");

        mockMvc.perform(post("/auth/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserPayload("intruder", "secret123", "ADMIN"))))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(403));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestPayload(username, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private record LoginRequestPayload(String username, String password) {
    }

    private record CreateUserPayload(String username, String password, String role) {
    }
}
