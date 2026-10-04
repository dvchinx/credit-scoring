package com.florez.backend.scoring.infrastructure.config;

import com.florez.backend.scoring.domain.model.DecisionPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Umbrales de la política de crédito, externalizados en configuración (nunca en código). */
@ConfigurationProperties(prefix = "credit-policy")
public record CreditPolicyProperties(
        double approveMaxProbability,
        double rejectMinProbability,
        double maxDebtRatio,
        int minApplicantAge) {

    public DecisionPolicy toDomain() {
        return new DecisionPolicy(approveMaxProbability, rejectMinProbability, maxDebtRatio, minApplicantAge);
    }
}
