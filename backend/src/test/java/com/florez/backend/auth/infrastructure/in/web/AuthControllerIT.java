package com.florez.backend.auth.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("token"));
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

    private record LoginRequestPayload(String username, String password) {
    }

    private record CreateUserPayload(String username, String password, String role) {
    }
}
