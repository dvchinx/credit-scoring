package com.florez.backend.scoring.domain.port.out;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;

/** Modelo de riesgo activo (servicio de ML externo). */
public interface ScoringModelPort {

    /** @throws ScoringUnavailableException si el modelo no responde o la respuesta es inválida */
    RiskAssessment score(ModelFeatures features);

    /** @throws ScoringUnavailableException si el modelo no responde o la respuesta es inválida */
    Explanation explain(ModelFeatures features);
}
