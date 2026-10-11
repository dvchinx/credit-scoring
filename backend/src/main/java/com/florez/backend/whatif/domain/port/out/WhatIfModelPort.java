package com.florez.backend.whatif.domain.port.out;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.whatif.domain.model.ModelEvaluation;

/** Evalúa dos entradas con exactamente la misma versión de modelo, en una sola llamada al servicio de ML. */
public interface WhatIfModelPort {

    /** @throws ScoringUnavailableException si el modelo no responde o la respuesta es inválida */
    Comparison compare(String modelVersion, ModelFeatures baseline, ModelFeatures scenario);

    record Comparison(ModelEvaluation baseline, ModelEvaluation scenario) {
    }
}
