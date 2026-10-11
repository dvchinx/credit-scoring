package com.florez.backend.whatif.domain.model;

/**
 * Cómo cambia una variable del modelo entre el escenario real y el simulado. Con un modelo
 * lineal, {@code shapDelta} es exactamente el efecto de ese cambio en el log-odds de default.
 */
public record FeatureImpact(
        String feature, double baselineValue, double scenarioValue, double baselineShap, double scenarioShap) {

    public double shapDelta() {
        return scenarioShap - baselineShap;
    }

    public boolean changed() {
        return Double.compare(baselineValue, scenarioValue) != 0;
    }
}
