package com.florez.backend.scoring.domain.model;

import java.util.Objects;

/**
 * Resultado del modelo: probabilidad de default, score 300-850 (a mayor score, menor riesgo)
 * y la versión exacta del modelo que lo calculó.
 */
public record RiskAssessment(double probabilityOfDefault, int riskScore, String modelVersion) {

    public RiskAssessment {
        if (probabilityOfDefault < 0 || probabilityOfDefault > 1) {
            throw new IllegalArgumentException("probabilityOfDefault fuera de [0, 1]: " + probabilityOfDefault);
        }
        Objects.requireNonNull(modelVersion, "modelVersion");
    }
}
