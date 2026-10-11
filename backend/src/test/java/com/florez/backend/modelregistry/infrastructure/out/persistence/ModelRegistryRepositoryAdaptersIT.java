package com.florez.backend.modelregistry.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.TestcontainersConfig;
import com.florez.backend.modelregistry.ModelRegistryTestData;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfig.class)
class ModelRegistryRepositoryAdaptersIT {

    @Autowired
    private ModelVersionRepositoryAdapter versions;

    @Autowired
    private ModelActivationRepositoryAdapter activations;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ModelVersion newVersion(String version) {
        return new ModelVersion(ModelRegistryTestData.artifact(version), "admin", Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void save_persisteYRecuperaLaMetadataCompleta() {
        ModelVersion original = newVersion("persist-it-1");

        versions.save(original);

        assertThat(versions.findByVersion("persist-it-1")).contains(original);
        assertThat(versions.exists("persist-it-1")).isTrue();
    }

    @Test
    void save_unaVersionYaRegistrada_nuncaLaSobreescribe() {
        versions.save(newVersion("persist-it-2"));
        ModelVersion attemptedOverwrite = new ModelVersion(
                ModelRegistryTestData.artifact("persist-it-2", "d".repeat(64)), "otro", Instant.now());

        assertThatThrownBy(() -> versions.save(attemptedOverwrite)).isInstanceOf(DataAccessException.class);
        assertThat(versions.findByVersion("persist-it-2").orElseThrow().artifact().artifactSha256())
                .isEqualTo(ModelRegistryTestData.ARTIFACT_SHA256);
    }

    @Test
    void findCurrent_esLaUltimaActivacionInsertada() {
        versions.save(newVersion("persist-it-3"));
        versions.save(newVersion("persist-it-4"));
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        activations.save(ModelActivation.create("persist-it-3", "release", "admin", now));
        ModelActivation last = activations.save(ModelActivation.create("persist-it-4", "nuevo", "admin", now));

        assertThat(activations.findCurrent()).contains(last);
        assertThat(activations.findAllNewestFirst().getFirst()).isEqualTo(last);
    }

    @Test
    void lasTablasDelRegistrySonAppendOnly() {
        versions.save(newVersion("persist-it-5"));
        ModelActivation activation = activations.save(ModelActivation.create("persist-it-5", "release", "admin", Instant.now()));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE model_versions SET artifact_sha256 = ? WHERE version = 'persist-it-5'", "e".repeat(64)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("model_versions es append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM model_activations WHERE id = ?", activation.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("model_activations es append-only");
    }

    @Test
    void unaActivacionDebeApuntarAUnaVersionRegistrada() {
        assertThatThrownBy(() -> activations.save(
                ModelActivation.create("nunca-registrada", "release", "admin", Instant.now())))
                .isInstanceOf(DataAccessException.class);
    }
}
