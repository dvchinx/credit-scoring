package com.florez.backend.scoring.domain.model;

/**
 * Aporte de una variable a la predicción, como valor SHAP en log-odds de default.
 *
 * @param feature   nombre canónico de la feature en el modelo
 * @param value     valor que recibió el modelo
 * @param shapValue contribución al log-odds (positivo = más riesgo)
 */
public record FeatureContribution(String feature, double value, double shapValue, RiskDirection direction) {
}
