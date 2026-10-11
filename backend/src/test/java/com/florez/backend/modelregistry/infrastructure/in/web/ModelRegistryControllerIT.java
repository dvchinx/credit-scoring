package com.florez.backend.modelregistry.infrastructure.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.florez.backend.TestcontainersConfig;
import com.florez.backend.modelregistry.ModelRegistryTestData;
import com.florez.backend.modelregistry.domain.model.CatalogEntry;
import com.florez.backend.modelregistry.domain.port.out.ModelArtifactCatalogPort;
import java.util.Map;
import java.util.Optional;
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

/** Registro y activación contra Postgres real; el catálogo del servicio de ML se sustituye por un mock. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class ModelRegistryControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ModelArtifactCatalogPort catalog;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "ChangeMe123!");
        when(catalog.find(anyString())).thenAnswer(invocation ->
                Optional.of(new CatalogEntry(ModelRegistryTestData.artifact(invocation.getArgument(0)), true)));
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private MvcResult postJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andReturn();
    }

    private MvcResult register(String version) throws Exception {
        return postJson("/model-versions", adminToken, Map.of("version", version));
    }

    private MvcResult activate(String version, String reason) throws Exception {
        return postJson("/model-activations", adminToken, Map.of("version", version, "reason", reason));
    }

    private JsonNode getJson(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void registrarYActivar_dejaLaVersionActivaConSuMetadataYAuditoria() throws Exception {
        MvcResult registered = register("reg-it-1");

        assertThat(registered.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = objectMapper.readTree(registered.getResponse().getContentAsString());
        assertThat(body.get("artifactSha256").asText()).isEqualTo(ModelRegistryTestData.ARTIFACT_SHA256);
        assertThat(body.get("dataset").get("sha256").asText()).isEqualTo(ModelRegistryTestData.DATASET_SHA256);
        assertThat(body.get("metrics").get("roc_auc").asDouble()).isEqualTo(0.86);
        assertThat(body.get("registeredBy").asText()).isEqualTo("admin");
        assertThat(body.get("active").asBoolean()).isFalse();

        MvcResult activation = activate("reg-it-1", "Primera versión en producción");

        assertThat(activation.getResponse().getStatus()).isEqualTo(201);
        JsonNode current = getJson("/model-activations/current");
        assertThat(current.get("modelVersion").asText()).isEqualTo("reg-it-1");
        assertThat(current.get("reason").asText()).isEqualTo("Primera versión en producción");
        assertThat(current.get("activatedBy").asText()).isEqualTo("admin");
        assertThat(getJson("/model-versions/reg-it-1").get("active").asBoolean()).isTrue();
    }

    @Test
    void rollback_esUnaActivacionNuevaYElHistorialConservaTodas() throws Exception {
        register("reg-it-2");
        register("reg-it-3");
        activate("reg-it-2", "release");
        activate("reg-it-3", "nuevo modelo");
        activate("reg-it-2", "rollback: reg-it-3 sube los rechazos");

        assertThat(getJson("/model-activations/current").get("modelVersion").asText()).isEqualTo("reg-it-2");
        JsonNode history = getJson("/model-activations");
        assertThat(history.get(0).get("reason").asText()).startsWith("rollback");
        assertThat(history.get(1).get("modelVersion").asText()).isEqualTo("reg-it-3");
        assertThat(history.get(2).get("modelVersion").asText()).isEqualTo("reg-it-2");
    }

    @Test
    void registrarDosVeces_devuelve409() throws Exception {
        register("reg-it-4");

        assertThat(register("reg-it-4").getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void registrarUnArtefactoAlterado_devuelve409() throws Exception {
        when(catalog.find("reg-it-5"))
                .thenReturn(Optional.of(new CatalogEntry(ModelRegistryTestData.artifact("reg-it-5"), false)));

        MvcResult result = register("reg-it-5");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("fue modificado");
    }

    @Test
    void activarSiElMlSirveOtroArtefacto_devuelve409YNoCambiaLaVersionActiva() throws Exception {
        register("reg-it-6");
        register("reg-it-7");
        activate("reg-it-6", "release");
        when(catalog.find("reg-it-7")).thenReturn(Optional.of(
                new CatalogEntry(ModelRegistryTestData.artifact("reg-it-7", "c".repeat(64)), true)));

        MvcResult result = activate("reg-it-7", "nuevo modelo");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(getJson("/model-activations/current").get("modelVersion").asText()).isEqualTo("reg-it-6");
    }

    @Test
    void activarSinMotivo_devuelve400() throws Exception {
        register("reg-it-8");

        assertThat(activate("reg-it-8", " ").getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void activarVersionNoRegistrada_devuelve404() throws Exception {
        assertThat(activate("nunca-registrada", "motivo").getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void unAnalistaPuedeConsultarPeroNoRegistrarNiActivar() throws Exception {
        postJson("/auth/users", adminToken, Map.of("username", "analyst-registry", "password", "secret123", "role", "ANALYST"));
        String analystToken = login("analyst-registry", "secret123");
        register("reg-it-9");

        assertThat(postJson("/model-versions", analystToken, Map.of("version", "reg-it-10")).getResponse().getStatus())
                .isEqualTo(403);
        assertThat(postJson("/model-activations", analystToken, Map.of("version", "reg-it-9", "reason", "x"))
                .getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(get("/model-versions").header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}
