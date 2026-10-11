package com.florez.backend.modelregistry.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlDatasetInfo(
        String source,
        String sha256,
        @JsonProperty("training_rows") Integer trainingRows,
        @JsonProperty("test_rows") Integer testRows,
        @JsonProperty("default_rate") Double defaultRate) {
}
