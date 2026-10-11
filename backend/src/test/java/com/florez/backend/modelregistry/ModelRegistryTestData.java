package com.florez.backend.modelregistry;

import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.model.ModelArtifact;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.model.TrainingDataset;
import com.florez.backend.modelregistry.domain.port.out.ModelActivationRepositoryPort;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import java.time.Instant;
import java.util.Map;

public final class ModelRegistryTestData {

    public static final String ARTIFACT_SHA256 = "a".repeat(64);
    public static final String DATASET_SHA256 = "b".repeat(64);

    private ModelRegistryTestData() {
    }

    public static ModelArtifact artifact(String version) {
        return artifact(version, ARTIFACT_SHA256);
    }

    public static ModelArtifact artifact(String version, String artifactSha256) {
        return new ModelArtifact(
                version,
                "SimpleImputer -> StandardScaler -> LogisticRegression",
                Map.of("C", 1.0, "class_weight", "balanced", "max_iter", 1000),
                Instant.parse("2026-10-04T12:00:00Z"),
                new TrainingDataset("cs-training.csv", DATASET_SHA256, 120000, 30000, 0.0668),
                Map.of("roc_auc", 0.86, "brier_score", 0.17),
                ModelFeatures.FEATURE_NAMES,
                artifactSha256);
    }

    /** Deja {@code version} registrada (si no lo estaba) y como versión activa. Para tests de integración. */
    public static void activate(
            String version, ModelVersionRepositoryPort versions, ModelActivationRepositoryPort activations) {
        if (!versions.exists(version)) {
            versions.save(new ModelVersion(artifact(version), "admin", Instant.now()));
        }
        activations.save(ModelActivation.create(version, "test", "admin", Instant.now()));
    }
}
