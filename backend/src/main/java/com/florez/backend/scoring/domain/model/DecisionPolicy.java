package com.florez.backend.scoring.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio que convierten el score del modelo en una decisión. Los umbrales llegan
 * desde configuración externa y se guardan con cada decisión, para saber qué política se
 * aplicó aunque cambie después.
 *
 * <p>Orden de evaluación:
 * <ol>
 *   <li>Reglas duras (edad mínima, endeudamiento máximo): si alguna falla, se rechaza sin
 *       importar el score.</li>
 *   <li>Probabilidad de default ≥ {@code rejectMinProbability}: rechazo.</li>
 *   <li>Probabilidad de default ≤ {@code approveMaxProbability}: aprobación.</li>
 *   <li>Entre ambos umbrales: revisión manual.</li>
 * </ol>
 */
public record DecisionPolicy(
        double approveMaxProbability,
        double rejectMinProbability,
        double maxDebtRatio,
        int minApplicantAge) {

    public DecisionPolicy {
        requireProbability("approveMaxProbability", approveMaxProbability);
        requireProbability("rejectMinProbability", rejectMinProbability);
        if (approveMaxProbability >= rejectMinProbability) {
            throw new IllegalArgumentException(
                    "approveMaxProbability (" + approveMaxProbability + ") debe ser menor que rejectMinProbability ("
                            + rejectMinProbability + ")");
        }
        if (maxDebtRatio <= 0) {
            throw new IllegalArgumentException("maxDebtRatio debe ser positivo: " + maxDebtRatio);
        }
        if (minApplicantAge <= 0) {
            throw new IllegalArgumentException("minApplicantAge debe ser positivo: " + minApplicantAge);
        }
    }

    public PolicyResult evaluate(ModelFeatures features, RiskAssessment risk) {
        List<String> hardRuleViolations = new ArrayList<>();
        if (features.age() < minApplicantAge) {
            hardRuleViolations.add(format("El solicitante tiene %d años; la edad mínima es %d", features.age(), minApplicantAge));
        }
        if (features.debtRatio() > maxDebtRatio) {
            hardRuleViolations.add(format(
                    "Endeudamiento con la nueva cuota de %.1f%% supera el máximo permitido de %.1f%%",
                    features.debtRatio() * 100, maxDebtRatio * 100));
        }
        if (!hardRuleViolations.isEmpty()) {
            return new PolicyResult(DecisionOutcome.REJECTED, hardRuleViolations);
        }

        double probability = risk.probabilityOfDefault();
        if (probability >= rejectMinProbability) {
            return new PolicyResult(DecisionOutcome.REJECTED, List.of(format(
                    "Probabilidad de default de %.1f%% alcanza el umbral de rechazo de %.1f%%",
                    probability * 100, rejectMinProbability * 100)));
        }
        if (probability <= approveMaxProbability) {
            return new PolicyResult(DecisionOutcome.APPROVED, List.of(format(
                    "Probabilidad de default de %.1f%% dentro del umbral de aprobación de %.1f%%",
                    probability * 100, approveMaxProbability * 100)));
        }
        return new PolicyResult(DecisionOutcome.MANUAL_REVIEW, List.of(format(
                "Probabilidad de default de %.1f%% entre el umbral de aprobación (%.1f%%) y el de rechazo (%.1f%%)",
                probability * 100, approveMaxProbability * 100, rejectMinProbability * 100)));
    }

    private static void requireProbability(String name, double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException(name + " debe estar en [0, 1]: " + value);
        }
    }

    private static String format(String template, Object... args) {
        return String.format(Locale.ROOT, template, args);
    }
}
