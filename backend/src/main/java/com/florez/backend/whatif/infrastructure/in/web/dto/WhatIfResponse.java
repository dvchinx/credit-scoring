package com.florez.backend.whatif.infrastructure.in.web.dto;

import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.whatif.domain.model.FeatureImpact;
import com.florez.backend.whatif.domain.model.ScenarioResult;
import com.florez.backend.whatif.domain.model.VariableChange;
import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WhatIfResponse(
        UUID id,
        UUID creditApplicationId,
        String modelVersion,
        List<ChangeResponse> changes,
        ScenarioResponse baseline,
        ScenarioResponse scenario,
        boolean decisionChanged,
        double probabilityOfDefaultDelta,
        int riskScoreDelta,
        double baseValue,
        List<FeatureImpactResponse> featureImpacts,
        PolicyResponse policy,
        String requestedBy,
        Instant simulatedAt) {

    public static WhatIfResponse from(WhatIfSimulation simulation) {
        return new WhatIfResponse(
                simulation.getId(),
                simulation.getCreditApplicationId(),
                simulation.getModelVersion(),
                simulation.getChanges().stream().map(ChangeResponse::from).toList(),
                ScenarioResponse.from(simulation.getBaseline()),
                ScenarioResponse.from(simulation.getScenario()),
                simulation.decisionChanged(),
                simulation.probabilityOfDefaultDelta(),
                simulation.riskScoreDelta(),
                simulation.getBaseline().explanation().baseValue(),
                simulation.featureImpacts().stream().map(FeatureImpactResponse::from).toList(),
                PolicyResponse.from(simulation.getPolicy()),
                simulation.getRequestedBy(),
                simulation.getSimulatedAt());
    }

    public record ChangeResponse(String variable, BigDecimal baselineValue, BigDecimal scenarioValue) {

        static ChangeResponse from(VariableChange change) {
            return new ChangeResponse(change.variable(), change.baselineValue(), change.scenarioValue());
        }
    }

    public record ScenarioResponse(
            DecisionOutcome outcome, double probabilityOfDefault, int riskScore, double outputValue, List<String> reasons) {

        static ScenarioResponse from(ScenarioResult result) {
            return new ScenarioResponse(
                    result.outcome(),
                    result.risk().probabilityOfDefault(),
                    result.risk().riskScore(),
                    result.explanation().outputValue(),
                    result.policyResult().reasons());
        }
    }

    /** Valores SHAP en log-odds de default; {@code shapDelta} es el efecto del cambio en esa variable. */
    public record FeatureImpactResponse(
            String feature,
            double baselineValue,
            double scenarioValue,
            double baselineShap,
            double scenarioShap,
            double shapDelta,
            boolean changed) {

        static FeatureImpactResponse from(FeatureImpact impact) {
            return new FeatureImpactResponse(impact.feature(), impact.baselineValue(), impact.scenarioValue(),
                    impact.baselineShap(), impact.scenarioShap(), impact.shapDelta(), impact.changed());
        }
    }

    public record PolicyResponse(
            double approveMaxProbability, double rejectMinProbability, double maxDebtRatio, int minApplicantAge) {

        static PolicyResponse from(DecisionPolicy policy) {
            return new PolicyResponse(policy.approveMaxProbability(), policy.rejectMinProbability(),
                    policy.maxDebtRatio(), policy.minApplicantAge());
        }
    }
}
