package com.florez.backend.scoring.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlFeatureContribution(
        String feature,
        Double value,
        @JsonProperty("shap_value") Double shapValue,
        String direction) {
}
