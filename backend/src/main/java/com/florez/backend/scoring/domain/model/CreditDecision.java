package com.florez.backend.scoring.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro inmutable de una evaluación de una solicitud de crédito. Contiene todo lo necesario
 * para reconstruir la decisión después: datos evaluados, entrada exacta del modelo, versión
 * del modelo, score, explicación SHAP y política aplicada.
 *
 * <p>Invariante: una decisión {@link DecisionStatus#COMPLETED} siempre tiene score y
 * explicación de la misma versión de modelo. Si no se puede explicar, la decisión solo puede
 * existir como {@link DecisionStatus#FAILED}.
 */
public final class CreditDecision {

    private final UUID id;
    private final UUID creditApplicationId;
    private final DecisionStatus status;
    private final DecisionOutcome outcome;
    private final ApplicationSnapshot applicationSnapshot;
    private final ModelFeatures modelInput;
    private final RiskAssessment riskAssessment;
    private final Explanation explanation;
    private final DecisionPolicy policy;
    private final List<String> reasons;
    private final String failureReason;
    private final String decidedBy;
    private final Instant decidedAt;

    private CreditDecision(
            UUID id,
            UUID creditApplicationId,
            DecisionStatus status,
            DecisionOutcome outcome,
            ApplicationSnapshot applicationSnapshot,
            ModelFeatures modelInput,
            RiskAssessment riskAssessment,
            Explanation explanation,
            DecisionPolicy policy,
            List<String> reasons,
            String failureReason,
            String decidedBy,
            Instant decidedAt) {
        this.id = id;
        this.creditApplicationId = Objects.requireNonNull(creditApplicationId, "creditApplicationId");
        this.status = Objects.requireNonNull(status, "status");
        this.outcome = outcome;
        this.applicationSnapshot = Objects.requireNonNull(applicationSnapshot, "applicationSnapshot");
        this.modelInput = Objects.requireNonNull(modelInput, "modelInput");
        this.riskAssessment = riskAssessment;
        this.explanation = explanation;
        this.policy = Objects.requireNonNull(policy, "policy");
        this.reasons = List.copyOf(reasons);
        this.failureReason = failureReason;
        this.decidedBy = Objects.requireNonNull(decidedBy, "decidedBy");
        this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");

        if (status == DecisionStatus.COMPLETED) {
            requireExplainedDecision();
        } else if (outcome != null || failureReason == null) {
            throw new IllegalArgumentException("Una decisión fallida no tiene resultado y debe indicar el motivo del fallo");
        }
    }

    private void requireExplainedDecision() {
        if (outcome == null || riskAssessment == null || explanation == null) {
            throw new IllegalArgumentException("Una decisión completada requiere resultado, score y explicación");
        }
        if (!riskAssessment.modelVersion().equals(explanation.modelVersion())) {
            throw new IllegalArgumentException("El score (modelo " + riskAssessment.modelVersion()
                    + ") y la explicación (modelo " + explanation.modelVersion() + ") deben venir de la misma versión");
        }
    }

    public static CreditDecision completed(
            UUID creditApplicationId,
            ApplicationSnapshot applicationSnapshot,
            ModelFeatures modelInput,
            RiskAssessment riskAssessment,
            Explanation explanation,
            DecisionPolicy policy,
            PolicyResult policyResult,
            String decidedBy,
            Instant decidedAt) {
        return new CreditDecision(
                null,
                creditApplicationId,
                DecisionStatus.COMPLETED,
                policyResult.outcome(),
                applicationSnapshot,
                modelInput,
                riskAssessment,
                explanation,
                policy,
                policyResult.reasons(),
                null,
                decidedBy,
                decidedAt);
    }

    public static CreditDecision failed(
            UUID creditApplicationId,
            ApplicationSnapshot applicationSnapshot,
            ModelFeatures modelInput,
            DecisionPolicy policy,
            String failureReason,
            String decidedBy,
            Instant decidedAt) {
        return new CreditDecision(
                null,
                creditApplicationId,
                DecisionStatus.FAILED,
                null,
                applicationSnapshot,
                modelInput,
                null,
                null,
                policy,
                List.of(),
                failureReason,
                decidedBy,
                decidedAt);
    }

    public static CreditDecision reconstitute(
            UUID id,
            UUID creditApplicationId,
            DecisionStatus status,
            DecisionOutcome outcome,
            ApplicationSnapshot applicationSnapshot,
            ModelFeatures modelInput,
            RiskAssessment riskAssessment,
            Explanation explanation,
            DecisionPolicy policy,
            List<String> reasons,
            String failureReason,
            String decidedBy,
            Instant decidedAt) {
        return new CreditDecision(
                id,
                creditApplicationId,
                status,
                outcome,
                applicationSnapshot,
                modelInput,
                riskAssessment,
                explanation,
                policy,
                reasons,
                failureReason,
                decidedBy,
                decidedAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreditApplicationId() {
        return creditApplicationId;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public DecisionOutcome getOutcome() {
        return outcome;
    }

    public ApplicationSnapshot getApplicationSnapshot() {
        return applicationSnapshot;
    }

    public ModelFeatures getModelInput() {
        return modelInput;
    }

    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }

    public Explanation getExplanation() {
        return explanation;
    }

    public DecisionPolicy getPolicy() {
        return policy;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
