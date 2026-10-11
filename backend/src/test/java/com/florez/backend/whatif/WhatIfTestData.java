package com.florez.backend.whatif;

import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.FeatureContribution;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.model.RiskDirection;
import com.florez.backend.whatif.domain.model.ModelEvaluation;
import java.util.Comparator;
import java.util.List;

public final class WhatIfTestData {

    private WhatIfTestData() {
    }

    /**
     * Evaluación de un "modelo lineal" de juguete: cada feature aporta coeficiente × valor, como en
     * el modelo real, así que un cambio en una variable solo mueve su propia contribución.
     */
    public static ModelEvaluation evaluation(ModelFeatures features, double probabilityOfDefault, String modelVersion) {
        List<FeatureContribution> contributions = features.asFeatureMap().entrySet().stream()
                .map(entry -> {
                    double value = entry.getValue().doubleValue();
                    double shap = coefficient(entry.getKey()) * value;
                    RiskDirection direction = shap > 0 ? RiskDirection.INCREASES_RISK
                            : shap < 0 ? RiskDirection.DECREASES_RISK : RiskDirection.NEUTRAL;
                    return new FeatureContribution(entry.getKey(), value, shap, direction);
                })
                .sorted(Comparator.comparingDouble((FeatureContribution c) -> Math.abs(c.shapValue())).reversed())
                .toList();
        double output = -0.5 + contributions.stream().mapToDouble(FeatureContribution::shapValue).sum();
        int riskScore = (int) Math.round(850 - probabilityOfDefault * 550);
        return new ModelEvaluation(
                new RiskAssessment(probabilityOfDefault, riskScore, modelVersion),
                new Explanation(modelVersion, -0.5, output, contributions));
    }

    private static double coefficient(String feature) {
        return switch (feature) {
            case "revolving_utilization_of_unsecured_lines" -> 1.2;
            case "debt_ratio" -> 0.8;
            case "monthly_income" -> -0.0001;
            case "age" -> -0.01;
            default -> 0.3;
        };
    }
}
