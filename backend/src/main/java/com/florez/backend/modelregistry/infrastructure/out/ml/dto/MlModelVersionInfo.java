package com.florez.backend.modelregistry.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record MlModelVersionInfo(
        String version,
        @JsonProperty("trained_at") String trainedAt,
        String algorithm,
        Map<String, Object> hyperparameters,
        MlDatasetInfo dataset,
        Map<String, Double> metrics,
        @JsonProperty("feature_names") List<String> featureNames,
        @JsonProperty("artifact_sha256") String artifactSha256,
        @JsonProperty("artifact_verified") Boolean artifactVerified) {
}
