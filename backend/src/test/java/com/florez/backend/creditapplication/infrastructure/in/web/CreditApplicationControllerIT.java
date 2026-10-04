package com.florez.backend.creditapplication.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.florez.backend.TestcontainersConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
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
class CreditApplicationControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        String loginBody = objectMapper.writeValueAsString(new LoginPayload("admin", "ChangeMe123!"));
        String response = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(response).get("token").asText();
    }

    private CreditApplicationPayload samplePayload(String documentId) {
        return new CreditApplicationPayload(
                "Alice Wonderland",
                documentId,
                LocalDate.of(1992, 3, 15),
                new BigDecimal("5000.00"),
                new BigDecimal("20000.00"),
                48,
                6.5,
                new BigDecimal("400.00"),
                0,
                new CreditHistoryPayload(0.25, 4, 1, 0, 0, 0));
    }

    @Test
    void flujoCompletoCrud_crearListarActualizarYEliminarSuave() throws Exception {
        String createBody = objectMapper.writeValueAsString(samplePayload("CTRL-DOC-1"));

        String createResponse = mockMvc.perform(post("/credit-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createResponse);
        String id = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("PENDING");

        mockMvc.perform(get("/credit-applications/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));

        String updateBody = objectMapper.writeValueAsString(samplePayload("CTRL-DOC-1-UPDATED"));
        mockMvc.perform(put("/credit-applications/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("CTRL-DOC-1-UPDATED"));

        mockMvc.perform(delete("/credit-applications/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));

        mockMvc.perform(get("/credit-applications/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    void create_sinToken_devuelve401() throws Exception {
        String body = objectMapper.writeValueAsString(samplePayload("CTRL-DOC-NOAUTH"));

        mockMvc.perform(post("/credit-applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }

    @Test
    void create_sinHistorialCrediticio_devuelve400() throws Exception {
        CreditApplicationPayload withoutHistory = new CreditApplicationPayload(
                "Alice Wonderland", "CTRL-DOC-NOHIST", LocalDate.of(1992, 3, 15), new BigDecimal("5000.00"),
                new BigDecimal("20000.00"), 48, 6.5, new BigDecimal("400.00"), 0, null);

        mockMvc.perform(post("/credit-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withoutHistory)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("creditHistory"));
    }

    @Test
    void create_conDocumentIdDuplicado_devuelve409() throws Exception {
        String body = objectMapper.writeValueAsString(samplePayload("CTRL-DOC-DUP"));

        mockMvc.perform(post("/credit-applications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/credit-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(409));
    }

    private record LoginPayload(String username, String password) {
    }

    private record CreditApplicationPayload(
            String applicantFullName,
            String documentId,
            LocalDate birthDate,
            BigDecimal monthlyIncome,
            BigDecimal requestedAmount,
            Integer loanTermMonths,
            Double employmentYears,
            BigDecimal existingMonthlyDebt,
            Integer numberOfDependents,
            CreditHistoryPayload creditHistory) {
    }

    private record CreditHistoryPayload(
            Double revolvingUtilization,
            Integer openCreditLines,
            Integer realEstateLoans,
            Integer latePayments30To59Days,
            Integer latePayments60To89Days,
            Integer latePayments90DaysOrMore) {
    }
}
