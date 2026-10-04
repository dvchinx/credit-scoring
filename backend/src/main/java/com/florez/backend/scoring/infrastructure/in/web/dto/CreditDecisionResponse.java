package com.florez.backend.scoring.infrastructure.in.web.dto;

import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.DecisionStatus;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.FeatureContribution;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.model.RiskDirection;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreditDecisionResponse(
        UUID id,
        UUID creditApplicationId,
        DecisionStatus status,
        DecisionOutcome outcome,
        Double probabilityOfDefault,
        Integer riskScore,
        String modelVersion,
        List<String> reasons,
        ExplanationResponse explanation,
        Map<String, Number> modelInput,
        PolicyResponse policy,
        String failureReason,
        String decidedBy,
        Instant decidedAt) {

    public static CreditDecisionResponse from(CreditDecision decision) {
        RiskAssessment risk = decision.getRiskAssessment();
        return new CreditDecisionResponse(
                decision.getId(),
                decision.getCreditApplicationId(),
                decision.getStatus(),
                decision.getOutcome(),
                risk == null ? null : risk.probabilityOfDefault(),
                risk == null ? null : risk.riskScore(),
                risk == null ? null : risk.modelVersion(),
                decision.getReasons(),
                decision.getExplanation() == null ? null : ExplanationResponse.from(decision.getExplanation()),
                decision.getModelInput().asFeatureMap(),
                PolicyResponse.from(decision.getPolicy()),
                decision.getFailureReason(),
                decision.getDecidedBy(),
                decision.getDecidedAt());
    }

    /** Valores SHAP en log-odds de default: {@code baseValue + Σ shapValue = outputValue}. */
    public record ExplanationResponse(double baseValue, double outputValue, List<FeatureContributionResponse> contributions) {

        static ExplanationResponse from(Explanation explanation) {
            return new ExplanationResponse(
                    explanation.baseValue(),
                    explanation.outputValue(),
                    explanation.contributions().stream().map(FeatureContributionResponse::from).toList());
        }
    }

    public record FeatureContributionResponse(String feature, double value, double shapValue, RiskDirection direction) {

        static FeatureContributionResponse from(FeatureContribution contribution) {
            return new FeatureContributionResponse(
                    contribution.feature(), contribution.value(), contribution.shapValue(), contribution.direction());
        }
    }

    public record PolicyResponse(
            double approveMaxProbability, double rejectMinProbability, double maxDebtRatio, int minApplicantAge) {

        static PolicyResponse from(DecisionPolicy policy) {
            return new PolicyResponse(
                    policy.approveMaxProbability(),
                    policy.rejectMinProbability(),
                    policy.maxDebtRatio(),
                    policy.minApplicantAge());
        }
    }
}
