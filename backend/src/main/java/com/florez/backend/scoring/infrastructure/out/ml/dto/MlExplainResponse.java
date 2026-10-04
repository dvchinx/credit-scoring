package com.florez.backend.scoring.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MlExplainResponse(
        @JsonProperty("model_version") String modelVersion,
        @JsonProperty("base_value") Double baseValue,
        @JsonProperty("output_value") Double outputValue,
        List<MlFeatureContribution> contributions) {
}
