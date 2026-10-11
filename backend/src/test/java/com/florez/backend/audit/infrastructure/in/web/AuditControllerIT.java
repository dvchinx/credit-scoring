package com.florez.backend.audit.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.modelregistry.ModelRegistryTestData;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import com.florez.backend.scoring.ScoringTestData;
import com.florez.backend.scoring.domain.model.ApplicationSnapshot;
import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.PolicyResult;
import com.florez.backend.scoring.domain.port.out.CreditDecisionRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** El resumen es global y otros tests escriben en la misma base: se comprueban diferencias, no totales. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AuditControllerIT {

    private static final String MODEL_VERSION = "audit-it-v1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreditApplicationRepositoryPort applications;

    @Autowired
    private CreditDecisionRepositoryPort decisions;

    @Autowired
    private ModelVersionRepositoryPort modelVersions;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"ChangeMe123!\"}"))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(login).get("token").asText();
        if (!modelVersions.exists(MODEL_VERSION)) {
            modelVersions.save(new ModelVersion(ModelRegistryTestData.artifact(MODEL_VERSION), "admin", Instant.now()));
        }
    }

    private JsonNode summary() throws Exception {
        String response = mockMvc.perform(get("/audit/summary").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private CreditApplication application(String documentId) {
        return applications.save(CreditApplication.createNew("Grace Hopper", documentId, LocalDate.of(1988, 12, 9),
                new BigDecimal("4200.00"), new BigDecimal("9000.00"), 18, 7.0, new BigDecimal("350.00"), 2,
                new CreditHistory(0.42, 7, 1, 2, 1, 0), "analyst1"));
    }

    private void completed(CreditApplication application, DecisionOutcome outcome) {
        decisions.save(CreditDecision.completed(application.getId(), ApplicationSnapshot.of(application),
                ModelFeatures.from(application, LocalDate.of(2026, 10, 18)), ScoringTestData.risk(0.2, MODEL_VERSION),
                ScoringTestData.explanation(MODEL_VERSION), ScoringTestData.POLICY,
                new PolicyResult(outcome, List.of("motivo")), "analyst1", Instant.now()));
    }

    private static long usage(JsonNode summary, String field) {
        for (JsonNode usage : summary.get("decisionsByModelVersion")) {
            if (usage.get("modelVersion").asText().equals(MODEL_VERSION)) {
                return usage.get(field).asLong();
            }
        }
        return 0;
    }

    @Test
    void summary_cuentaDecisionesPorResultadoYPorVersionDeModelo() throws Exception {
        JsonNode before = summary();
        CreditApplication application = application("AUDIT-1");

        completed(application, DecisionOutcome.APPROVED);
        completed(application, DecisionOutcome.REJECTED);
        completed(application, DecisionOutcome.REJECTED);

        JsonNode after = summary();
        assertThat(after.get("completedDecisions").asLong() - before.get("completedDecisions").asLong()).isEqualTo(3);
        assertThat(after.get("decisionsByOutcome").get("REJECTED").asLong()
                - before.get("decisionsByOutcome").get("REJECTED").asLong()).isEqualTo(2);
        assertThat(after.get("decisionsByOutcome").has("MANUAL_REVIEW")).isTrue();
        assertThat(usage(after, "rejected") - usage(before, "rejected")).isEqualTo(2);
        assertThat(usage(after, "total") - usage(before, "total")).isEqualTo(3);
    }

    @Test
    void summary_listaLosIntentosFallidosMasRecientesConSuMotivo() throws Exception {
        JsonNode before = summary();
        CreditApplication application = application("AUDIT-2");

        decisions.save(CreditDecision.failed(application.getId(), ApplicationSnapshot.of(application),
                ModelFeatures.from(application, LocalDate.of(2026, 10, 18)), ScoringTestData.POLICY,
                "el servicio de ML no respondió", "analyst1", Instant.now()));

        JsonNode after = summary();
        assertThat(after.get("failedDecisions").asLong() - before.get("failedDecisions").asLong()).isEqualTo(1);
        JsonNode latest = after.get("recentFailures").get(0);
        assertThat(latest.get("creditApplicationId").asText()).isEqualTo(application.getId().toString());
        assertThat(latest.get("failureReason").asText()).isEqualTo("el servicio de ML no respondió");
        assertThat(after.get("simulations").has("outcomeChanged")).isTrue();
    }

    @Test
    void summary_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/audit/summary"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(401));
    }
}
