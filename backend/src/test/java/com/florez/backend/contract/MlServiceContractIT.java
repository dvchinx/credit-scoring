package com.florez.backend.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Contrato real entre el backend y el servicio de ML: nada de mocks. El servicio se construye desde
 * su Dockerfile, entrena al arrancar un modelo sobre el dataset sintético de demo y el backend le
 * habla por HTTP. Detecta lo que los tests con MockRestServiceServer no pueden ver: protocolo HTTP,
 * nombres de campos en ambos lados, formato de fechas y versiones, y el flujo completo de punta a punta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@Testcontainers
class MlServiceContractIT {

    // withDockerfile usa la carpeta del Dockerfile como contexto y respeta su .dockerignore (sin .venv ni
    // modelos). La imagen se conserva (deleteOnExit=false) para reutilizar la caché de capas.
    @Container
    static final GenericContainer<?> ML_SERVICE = new GenericContainer<>(
            new ImageFromDockerfile("credit-scoring-ml-service-it", false)
                    .withDockerfile(Path.of("..", "ml-service", "Dockerfile").toAbsolutePath().normalize()))
            .withCommand("sh", "-c", String.join(" && ",
                    "python -m training.synthetic --output /tmp/demo.csv --rows 6000",
                    "python -m training.train --input /tmp/demo.csv --output-dir /app/models",
                    "uvicorn app.main:app --host 0.0.0.0 --port 8000"))
            .withExposedPorts(8000)
            .waitingFor(Wait.forHttp("/health").forResponsePredicate(body -> body.contains("\"ok\""))
                    .withStartupTimeout(Duration.ofMinutes(5)));

    @DynamicPropertySource
    static void mlServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("ml-service.base-url", MlServiceContractIT::mlServiceUrl);
    }

    private static String mlServiceUrl() {
        return "http://" + ML_SERVICE.getHost() + ":" + ML_SERVICE.getMappedPort(8000);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void loginAndActivateTrainedModel() throws Exception {
        token = objectMapper.readTree(perform(post("/auth/login"), Map.of("username", "admin", "password", "ChangeMe123!"))
                .getResponse().getContentAsString()).get("token").asText();

        String version = trainedVersion();
        if (perform(get("/model-versions/" + version), null).getResponse().getStatus() == 404) {
            MvcResult registered = perform(post("/model-versions"), Map.of("version", version));
            assertThat(registered.getResponse().getStatus()).as(registered.getResponse().getContentAsString()).isEqualTo(201);
            perform(post("/model-activations"), Map.of("version", version, "reason", "contract test"));
        }
    }

    private static String trainedVersion() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            String body = client.send(HttpRequest.newBuilder(URI.create(mlServiceUrl() + "/models")).build(),
                    HttpResponse.BodyHandlers.ofString()).body();
            return new ObjectMapper().readTree(body).get(0).get("version").asText();
        }
    }

    private MvcResult perform(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, Object body) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request).andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String createApplication(String documentId, double income, double utilization, int late30, int late60, int late90)
            throws Exception {
        Map<String, Object> history = Map.of(
                "revolvingUtilization", utilization, "openCreditLines", 6, "realEstateLoans", 1,
                "latePayments30To59Days", late30, "latePayments60To89Days", late60, "latePayments90DaysOrMore", late90);
        Map<String, Object> application = Map.of(
                "applicantFullName", "Contrato " + documentId, "documentId", documentId, "birthDate", "1985-03-15",
                "monthlyIncome", income, "requestedAmount", 6000, "loanTermMonths", 24, "employmentYears", 5,
                "existingMonthlyDebt", 150, "numberOfDependents", 1, "creditHistory", history);
        MvcResult created = perform(post("/credit-applications"), application);
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        return json(created).get("id").asText();
    }

    private JsonNode evaluate(String applicationId) throws Exception {
        MvcResult result = perform(post("/credit-applications/" + applicationId + "/decisions"), null);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return json(result);
    }

    @Test
    void evaluarContraElModeloReal_produceDecisionesExplicadasYCoherentes() throws Exception {
        JsonNode lowRisk = evaluate(createApplication("CONTRACT-LOW", 7000, 0.05, 0, 0, 0));
        JsonNode highRisk = evaluate(createApplication("CONTRACT-HIGH", 2500, 0.95, 3, 2, 2));

        for (JsonNode decision : new JsonNode[] {lowRisk, highRisk}) {
            assertThat(decision.get("status").asText()).isEqualTo("COMPLETED");
            assertThat(decision.get("modelVersion").asText()).isEqualTo(trainedVersion());
            assertThat(decision.get("explanation").get("contributions")).hasSize(ModelFeatures.FEATURE_NAMES.size());
            double shapSum = 0;
            for (JsonNode contribution : decision.get("explanation").get("contributions")) {
                shapSum += contribution.get("shapValue").asDouble();
            }
            // Exactitud local de SHAP a través de toda la cadena: base + Σ φ = log-odds de la probabilidad devuelta.
            double outputValue = decision.get("explanation").get("outputValue").asDouble();
            assertThat(decision.get("explanation").get("baseValue").asDouble() + shapSum)
                    .isCloseTo(outputValue, org.assertj.core.data.Offset.offset(1e-6));
            assertThat(1 / (1 + Math.exp(-outputValue)))
                    .isCloseTo(decision.get("probabilityOfDefault").asDouble(), org.assertj.core.data.Offset.offset(1e-6));
        }
        assertThat(highRisk.get("probabilityOfDefault").asDouble()).isGreaterThan(lowRisk.get("probabilityOfDefault").asDouble());
        assertThat(highRisk.get("explanation").get("contributions").get(0).get("direction").asText()).isEqualTo("INCREASES_RISK");
    }

    @Test
    void whatIfContraElModeloReal_quitarLasMorasBajaElRiesgoSinCrearDecisiones() throws Exception {
        String applicationId = createApplication("CONTRACT-WHATIF", 2500, 0.9, 3, 2, 2);
        evaluate(applicationId);

        MvcResult result = perform(post("/credit-applications/" + applicationId + "/what-if"),
                Map.of("creditHistory", Map.of("latePayments30To59Days", 0, "latePayments60To89Days", 0, "latePayments90DaysOrMore", 0)));

        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        JsonNode simulation = json(result);
        assertThat(simulation.get("probabilityOfDefaultDelta").asDouble()).isNegative();
        assertThat(simulation.get("modelVersion").asText()).isEqualTo(trainedVersion());
        for (JsonNode impact : simulation.get("featureImpacts")) {
            boolean lateFeature = impact.get("feature").asText().contains("past_due") || impact.get("feature").asText().contains("90_days");
            // Modelo lineal: solo cambia la contribución de las variables modificadas.
            assertThat(Math.abs(impact.get("shapDelta").asDouble()) > 1e-12).isEqualTo(lateFeature);
        }
        assertThat(json(perform(get("/credit-applications/" + applicationId + "/decisions"), null))).hasSize(1);
    }
}
