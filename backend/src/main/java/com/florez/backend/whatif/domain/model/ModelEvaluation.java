package com.florez.backend.whatif.domain.model;

import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.RiskAssessment;

/** Score y explicación de una entrada, calculados con la misma versión de modelo. */
public record ModelEvaluation(RiskAssessment risk, Explanation explanation) {
}
