package com.florez.backend.scoring.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import com.florez.backend.scoring.ScoringTestData;
import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Flujo completo contra Postgres real; solo el servicio de ML externo se sustituye por un mock. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CreditDecisionControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScoringModelPort scoringModel;

    private String token;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"ChangeMe123!\"}"))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(response).get("token").asText();
    }

    private String createApplication(String documentId, String requestedAmount) throws Exception {
        String body = """
                {
                  "applicantFullName": "Ada Lovelace",
                  "documentId": "%s",
                  "birthDate": "1990-05-10",
                  "monthlyIncome": 3500.00,
                  "requestedAmount": %s,
                  "loanTermMonths": 24,
                  "employmentYears": 4.5,
                  "existingMonthlyDebt": 200.00,
                  "numberOfDependents": 1,
                  "creditHistory": {
                    "revolvingUtilization": 0.3,
                    "openCreditLines": 5,
                    "realEstateLoans": 1,
                    "latePayments30To59Days": 0,
                    "latePayments60To89Days": 0,
                    "latePayments90DaysOrMore": 0
                  }
                }
                """.formatted(documentId, requestedAmount);
        String response = mockMvc.perform(post("/credit-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private JsonNode getJson(String path) throws Exception {
        String response = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    @Test
    void evaluate_conBajoRiesgo_apruebaYPersisteLaExplicacion() throws Exception {
        when(scoringModel.score(any())).thenReturn(ScoringTestData.risk(0.12, "20261004T120000Z"));
        when(scoringModel.explain(any())).thenReturn(ScoringTestData.explanation("20261004T120000Z"));
        String applicationId = createApplication("DEC-CTRL-1", "12000.00");

        String response = mockMvc.perform(post("/credit-applications/" + applicationId + "/decisions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201))
                .andReturn().getResponse().getContentAsString();

        JsonNode decision = objectMapper.readTree(response);
        assertThat(decision.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(decision.get("outcome").asText()).isEqualTo("APPROVED");
        assertThat(decision.get("modelVersion").asText()).isEqualTo("20261004T120000Z");
        assertThat(decision.get("riskScore").asInt()).isEqualTo(784);
        assertThat(decision.get("decidedBy").asText()).isEqualTo("admin");
        assertThat(decision.get("explanation").get("contributions")).hasSize(3);
        assertThat(decision.get("explanation").get("contributions").get(0).get("direction").asText())
                .isEqualTo("INCREASES_RISK");
        assertThat(decision.get("modelInput").get("debt_ratio").asDouble()).isEqualTo(700.0 / 3500.0);
        assertThat(decision.get("policy").get("approveMaxProbability").asDouble()).isEqualTo(0.30);
        assertThat(decision.get("reasons")).hasSize(1);

        assertThat(getJson("/credit-applications/" + applicationId).get("status").asText()).isEqualTo("APPROVED");
        JsonNode history = getJson("/credit-applications/" + applicationId + "/decisions");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("id").asText()).isEqualTo(decision.get("id").asText());
    }

    @Test
    void evaluate_conEndeudamientoExcesivo_rechazaPorReglaDuraAunqueElModeloApruebe() throws Exception {
        when(scoringModel.score(any())).thenReturn(ScoringTestData.risk(0.05, "v1"));
        when(scoringModel.explain(any())).thenReturn(ScoringTestData.explanation("v1"));
        // (200 + 60000 / 24) / 3500 = 77% de endeudamiento
        String applicationId = createApplication("DEC-CTRL-2", "60000.00");

        String response = mockMvc.perform(post("/credit-applications/" + applicationId + "/decisions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();

        JsonNode decision = objectMapper.readTree(response);
        assertThat(decision.get("outcome").asText()).isEqualTo("REJECTED");
        assertThat(decision.get("reasons").get(0).asText()).contains("Endeudamiento");
        assertThat(decision.get("explanation")).isNotNull();
    }

    @Test
    void evaluate_conServicioDeMlCaido_devuelve503YRegistraElIntentoFallido() throws Exception {
        when(scoringModel.score(any())).thenThrow(new ScoringUnavailableException("el servicio de ML no respondió"));
        String applicationId = createApplication("DEC-CTRL-3", "12000.00");

        mockMvc.perform(post("/credit-applications/" + applicationId + "/decisions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(503))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("el servicio de ML no respondió"));

        assertThat(getJson("/credit-applications/" + applicationId).get("status").asText()).isEqualTo("PENDING");
        JsonNode history = getJson("/credit-applications/" + applicationId + "/decisions");
        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("status").asText()).isEqualTo("FAILED");
        assertThat(history.get(0).get("outcome").isNull()).isTrue();
        assertThat(history.get(0).get("explanation").isNull()).isTrue();
        assertThat(history.get(0).get("failureReason").asText()).contains("no respondió");
    }

    @Test
    void reevaluar_creaUnaDecisionNuevaYConservaLaAnterior() throws Exception {
        when(scoringModel.score(any()))
                .thenReturn(ScoringTestData.risk(0.45, "v1"))
                .thenReturn(ScoringTestData.risk(0.10, "v2"));
        when(scoringModel.explain(any()))
                .thenReturn(ScoringTestData.explanation("v1"))
                .thenReturn(ScoringTestData.explanation("v2"));
        String applicationId = createApplication("DEC-CTRL-4", "12000.00");
        String decisionsPath = "/credit-applications/" + applicationId + "/decisions";

        mockMvc.perform(post(decisionsPath).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        mockMvc.perform(post(decisionsPath).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        JsonNode history = getJson(decisionsPath);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("modelVersion").asText()).isEqualTo("v2");
        assertThat(history.get(0).get("outcome").asText()).isEqualTo("APPROVED");
        assertThat(history.get(1).get("modelVersion").asText()).isEqualTo("v1");
        assertThat(history.get(1).get("outcome").asText()).isEqualTo("MANUAL_REVIEW");
        assertThat(getJson("/credit-applications/" + applicationId).get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    void evaluate_conSolicitudInexistente_devuelve404() throws Exception {
        mockMvc.perform(post("/credit-applications/" + UUID.randomUUID() + "/decisions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    void evaluate_sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/credit-applications/" + UUID.randomUUID() + "/decisions"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }
}
