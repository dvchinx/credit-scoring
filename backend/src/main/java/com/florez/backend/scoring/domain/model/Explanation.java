package com.florez.backend.scoring.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Explicación SHAP de una predicción: {@code baseValue + Σ shapValue = outputValue}, todo en
 * log-odds de default. Las contribuciones vienen ordenadas por magnitud descendente.
 */
public record Explanation(String modelVersion, double baseValue, double outputValue, List<FeatureContribution> contributions) {

    public Explanation {
        Objects.requireNonNull(modelVersion, "modelVersion");
        if (contributions == null || contributions.isEmpty()) {
            throw new IllegalArgumentException("Una explicación debe incluir al menos una contribución");
        }
        contributions = List.copyOf(contributions);
    }
}
