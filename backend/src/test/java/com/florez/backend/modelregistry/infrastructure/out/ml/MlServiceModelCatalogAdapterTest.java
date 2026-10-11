package com.florez.backend.modelregistry.infrastructure.out.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.florez.backend.common.domain.ServiceUnavailableException;
import com.florez.backend.modelregistry.domain.exception.InvalidModelArtifactException;
import com.florez.backend.modelregistry.domain.model.CatalogEntry;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MlServiceModelCatalogAdapterTest {

    private static final String FEATURES_JSON = ModelFeatures.FEATURE_NAMES.stream()
            .map(name -> "\"" + name + "\"")
            .collect(Collectors.joining(", ", "[", "]"));

    private MockRestServiceServer server;
    private MlServiceModelCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml-service");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new MlServiceModelCatalogAdapter(builder.build());
    }

    private static String completeModelJson(boolean verified) {
        return """
                {"version": "20261004T120000Z",
                 "trained_at": "2026-10-04T12:00:05.123456+00:00",
                 "algorithm": "SimpleImputer -> StandardScaler -> LogisticRegression",
                 "hyperparameters": {"C": 1.0, "class_weight": "balanced", "max_iter": 1000, "l1_ratio": null},
                 "dataset": {"source": "cs-training.csv", "sha256": "%s", "training_rows": 120000,
                             "test_rows": 30000, "default_rate": 0.0668},
                 "metrics": {"roc_auc": 0.86, "brier_score": 0.17},
                 "feature_names": %s,
                 "artifact_sha256": "%s",
                 "artifact_verified": %s}
                """.formatted("b".repeat(64), FEATURES_JSON, "a".repeat(64), verified);
    }

    @Test
    void find_mapeaLaMetadataCompleta() {
        server.expect(requestTo("http://ml-service/models/20261004T120000Z"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(completeModelJson(true), MediaType.APPLICATION_JSON));

        CatalogEntry entry = adapter.find("20261004T120000Z").orElseThrow();

        assertThat(entry.artifactVerified()).isTrue();
        assertThat(entry.artifact().trainedAt()).isEqualTo(Instant.parse("2026-10-04T12:00:05.123456Z"));
        assertThat(entry.artifact().dataset().trainingRows()).isEqualTo(120000);
        assertThat(entry.artifact().metrics()).containsEntry("roc_auc", 0.86);
        assertThat(entry.artifact().hyperparameters()).containsEntry("class_weight", "balanced").containsEntry("l1_ratio", null);
        assertThat(entry.artifact().featureNames()).containsExactlyElementsOf(ModelFeatures.FEATURE_NAMES);
        assertThat(entry.artifact().artifactSha256()).isEqualTo("a".repeat(64));
    }

    @Test
    void find_reportaSiElArtefactoNoEstaVerificado() {
        server.expect(requestTo("http://ml-service/models/20261004T120000Z"))
                .andRespond(withSuccess(completeModelJson(false), MediaType.APPLICATION_JSON));

        assertThat(adapter.find("20261004T120000Z").orElseThrow().artifactVerified()).isFalse();
    }

    @Test
    void find_versionInexistente_devuelveVacio() {
        server.expect(requestTo("http://ml-service/models/v9")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(adapter.find("v9")).isEmpty();
    }

    @Test
    void find_versionConFormatoInvalido_noLlamaAlServicio() {
        assertThat(adapter.find("../secrets")).isEqualTo(Optional.empty());
        server.verify();
    }

    @Test
    void find_versionLegacySinMetadataCompleta_explicaQueFalta() {
        server.expect(requestTo("http://ml-service/models/v-old"))
                .andRespond(withSuccess("""
                        {"version": "v-old", "trained_at": "2026-09-01T10:00:00+00:00",
                         "metrics": {"roc_auc": 0.85}, "feature_names": ["age"], "artifact_verified": false}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.find("v-old"))
                .isInstanceOf(InvalidModelArtifactException.class)
                .hasMessageContaining("algorithm")
                .hasMessageContaining("artifact_sha256")
                .hasMessageContaining("dataset")
                .hasMessageContaining("Reentrena");
    }

    @Test
    void find_conServicioCaido_lanzaServiceUnavailable() {
        server.expect(requestTo("http://ml-service/models/v1")).andRespond(withException(new IOException("refused")));

        assertThatThrownBy(() -> adapter.find("v1")).isInstanceOf(ServiceUnavailableException.class);
    }
}
