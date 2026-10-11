package com.florez.backend.scoring.domain.port.out;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;

/** Modelos de riesgo servidos por el servicio de ML externo. La versión siempre se indica explícitamente. */
public interface ScoringModelPort {

    /** @throws ScoringUnavailableException si el modelo no responde o la respuesta es inválida */
    RiskAssessment score(String modelVersion, ModelFeatures features);

    /** @throws ScoringUnavailableException si el modelo no responde o la respuesta es inválida */
    Explanation explain(String modelVersion, ModelFeatures features);
}
