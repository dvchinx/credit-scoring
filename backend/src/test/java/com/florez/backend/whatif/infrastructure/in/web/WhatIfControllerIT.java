package com.florez.backend.whatif.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import com.florez.backend.modelregistry.ModelRegistryTestData;
import com.florez.backend.modelregistry.domain.port.out.ModelActivationRepositoryPort;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.whatif.WhatIfTestData;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort.Comparison;
import java.util.Map;
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
import org.springframework.test.web.servlet.MvcResult;

/** Simulación completa contra Postgres real; solo el servicio de ML se sustituye por un mock. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class WhatIfControllerIT {

    private static final String MODEL_VERSION = "whatif-it-v1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModelVersionRepositoryPort modelVersions;

    @Autowired
    private ModelActivationRepositoryPort modelActivations;

    @MockitoBean
    private WhatIfModelPort model;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"ChangeMe123!\"}"))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(login).get("token").asText();
        ModelRegistryTestData.activate(MODEL_VERSION, modelVersions, modelActivations);
        // Riesgo según la utilización de crédito rotativo: más utilización, más riesgo.
        when(model.compare(anyString(), any(), any())).thenAnswer(invocation -> new Comparison(
                evaluationFor(invocation.getArgument(0), invocation.getArgument(1)),
                evaluationFor(invocation.getArgument(0), invocation.getArgument(2))));
    }

    private static com.florez.backend.whatif.domain.model.ModelEvaluation evaluationFor(String version, ModelFeatures f) {
        double pd = Math.min(0.95, 0.10 + f.revolvingUtilizationOfUnsecuredLines());
        return WhatIfTestData.evaluation(f, pd, version);
    }

    private String createApplication(String documentId) throws Exception {
        String body = """
                {"applicantFullName": "Ada Lovelace", "documentId": "%s", "birthDate": "1990-05-10",
                 "monthlyIncome": 3500.00, "requestedAmount": 12000.00, "loanTermMonths": 24,
                 "employmentYears": 4.5, "existingMonthlyDebt": 200.00, "numberOfDependents": 1,
                 "creditHistory": {"revolvingUtilization": 0.35, "openCreditLines": 5, "realEstateLoans": 1,
                   "latePayments30To59Days": 0, "latePayments60To89Days": 0, "latePayments90DaysOrMore": 0}}
                """.formatted(documentId);
        String response = mockMvc.perform(post("/credit-applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private MvcResult simulate(String applicationId, Object changes) throws Exception {
        return mockMvc.perform(post("/credit-applications/" + applicationId + "/what-if")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changes)))
                .andReturn();
    }

    private JsonNode getJson(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void simular_bajarLaUtilizacion_muestraQueLaDecisionPasariaAAprobada() throws Exception {
        String applicationId = createApplication("WHATIF-1");

        MvcResult result = simulate(applicationId, Map.of("creditHistory", Map.of("revolvingUtilization", 0.05)));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("modelVersion").asText()).isEqualTo(MODEL_VERSION);
        assertThat(body.get("baseline").get("outcome").asText()).isEqualTo("MANUAL_REVIEW");
        assertThat(body.get("scenario").get("outcome").asText()).isEqualTo("APPROVED");
        assertThat(body.get("decisionChanged").asBoolean()).isTrue();
        assertThat(body.get("probabilityOfDefaultDelta").asDouble()).isEqualTo(-0.30, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(body.get("changes").get(0).get("variable").asText()).isEqualTo("creditHistory.revolvingUtilization");
        assertThat(body.get("featureImpacts").get(0).get("feature").asText())
                .isEqualTo("revolving_utilization_of_unsecured_lines");
        assertThat(body.get("featureImpacts").get(0).get("changed").asBoolean()).isTrue();
        assertThat(body.get("requestedBy").asText()).isEqualTo("admin");
    }

    @Test
    void simular_noCambiaLaSolicitudNiCreaDecisionesPeroQuedaRegistrado() throws Exception {
        String applicationId = createApplication("WHATIF-2");

        simulate(applicationId, Map.of("monthlyIncome", 5000));
        simulate(applicationId, Map.of("requestedAmount", 6000));

        assertThat(getJson("/credit-applications/" + applicationId).get("status").asText()).isEqualTo("PENDING");
        assertThat(getJson("/credit-applications/" + applicationId + "/decisions")).isEmpty();
        JsonNode history = getJson("/credit-applications/" + applicationId + "/what-if");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("changes").get(0).get("variable").asText()).isEqualTo("requestedAmount");
        assertThat(history.get(1).get("changes").get(0).get("variable").asText()).isEqualTo("monthlyIncome");
    }

    @Test
    void simular_unMontoQueSuperaElEndeudamientoMaximo_seRechazaPorReglaDura() throws Exception {
        String applicationId = createApplication("WHATIF-3");

        // (200 + 60000 / 24) / 3500 = 77% de endeudamiento
        JsonNode body = objectMapper.readTree(simulate(applicationId, Map.of("requestedAmount", 60000))
                .getResponse().getContentAsString());

        assertThat(body.get("scenario").get("outcome").asText()).isEqualTo("REJECTED");
        assertThat(body.get("scenario").get("reasons").get(0).asText()).contains("Endeudamiento");
    }

    @Test
    void simular_sinCambios_devuelve400() throws Exception {
        String applicationId = createApplication("WHATIF-4");

        MvcResult result = simulate(applicationId, Map.of());

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("al menos una variable");
    }

    @Test
    void simular_conValoresInvalidos_devuelve400() throws Exception {
        String applicationId = createApplication("WHATIF-5");

        assertThat(simulate(applicationId, Map.of("monthlyIncome", -10)).getResponse().getStatus()).isEqualTo(400);
        assertThat(simulate(applicationId, Map.of("creditHistory", Map.of("latePayments90DaysOrMore", -1)))
                .getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void simular_solicitudInexistente_devuelve404() throws Exception {
        assertThat(simulate(UUID.randomUUID().toString(), Map.of("monthlyIncome", 5000)).getResponse().getStatus())
                .isEqualTo(404);
    }
}
