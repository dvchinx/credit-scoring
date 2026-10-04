package com.florez.backend.scoring.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlScoreResponse(
        @JsonProperty("probability_of_default") Double probabilityOfDefault,
        @JsonProperty("risk_score") Integer riskScore,
        @JsonProperty("model_version") String modelVersion) {
}
