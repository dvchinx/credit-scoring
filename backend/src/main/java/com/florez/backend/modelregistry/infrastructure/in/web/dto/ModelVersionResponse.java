package com.florez.backend.modelregistry.infrastructure.in.web.dto;

import com.florez.backend.modelregistry.domain.model.ModelArtifact;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.model.TrainingDataset;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ModelVersionResponse(
        String version,
        boolean active,
        String algorithm,
        Map<String, Object> hyperparameters,
        Instant trainedAt,
        DatasetResponse dataset,
        Map<String, Double> metrics,
        List<String> featureNames,
        String artifactSha256,
        String registeredBy,
        Instant registeredAt) {

    public static ModelVersionResponse from(ModelVersion modelVersion, boolean active) {
        ModelArtifact artifact = modelVersion.artifact();
        return new ModelVersionResponse(
                artifact.version(),
                active,
                artifact.algorithm(),
                artifact.hyperparameters(),
                artifact.trainedAt(),
                DatasetResponse.from(artifact.dataset()),
                artifact.metrics(),
                artifact.featureNames(),
                artifact.artifactSha256(),
                modelVersion.registeredBy(),
                modelVersion.registeredAt());
    }

    public record DatasetResponse(String source, String sha256, int trainingRows, int testRows, double defaultRate) {

        static DatasetResponse from(TrainingDataset dataset) {
            return new DatasetResponse(
                    dataset.source(), dataset.sha256(), dataset.trainingRows(), dataset.testRows(), dataset.defaultRate());
        }
    }
}
