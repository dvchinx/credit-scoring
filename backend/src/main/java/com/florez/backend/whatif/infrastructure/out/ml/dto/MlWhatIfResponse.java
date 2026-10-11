package com.florez.backend.whatif.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MlWhatIfResponse(
        @JsonProperty("model_version") String modelVersion,
        MlEvaluation baseline,
        MlEvaluation scenario) {
}
