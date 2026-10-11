package com.florez.backend.whatif.infrastructure.out.ml.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlFeatureContribution;
import java.util.List;

public record MlEvaluation(
        @JsonProperty("probability_of_default") Double probabilityOfDefault,
        @JsonProperty("risk_score") Integer riskScore,
        @JsonProperty("base_value") Double baseValue,
        @JsonProperty("output_value") Double outputValue,
        List<MlFeatureContribution> contributions) {
}
