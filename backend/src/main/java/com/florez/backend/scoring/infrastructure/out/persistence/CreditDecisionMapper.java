package com.florez.backend.scoring.infrastructure.out.persistence;

import static com.florez.backend.common.infrastructure.persistence.JsonColumnCodec.read;
import static com.florez.backend.common.infrastructure.persistence.JsonColumnCodec.write;

import com.fasterxml.jackson.core.type.TypeReference;
import com.florez.backend.scoring.domain.model.ApplicationSnapshot;
import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import java.util.List;
import java.util.Map;

/**
 * Traduce entre {@link CreditDecision} y su fila. Los snapshots se guardan como JSONB; la
 * entrada del modelo usa los nombres canónicos de feature, de modo que el JSON almacenado es
 * exactamente el payload que se envió a {@code /score} y {@code /explain}.
 */
final class CreditDecisionMapper {

    private CreditDecisionMapper() {
    }

    static CreditDecisionJpaEntity toNewEntity(CreditDecision decision) {
        RiskAssessment risk = decision.getRiskAssessment();
        return new CreditDecisionJpaEntity(
                decision.getCreditApplicationId(),
                decision.getStatus(),
                decision.getOutcome(),
                risk == null ? null : risk.probabilityOfDefault(),
                risk == null ? null : risk.riskScore(),
                risk == null ? null : risk.modelVersion(),
                write(decision.getApplicationSnapshot()),
                write(decision.getModelInput().asFeatureMap()),
                decision.getExplanation() == null ? null : write(decision.getExplanation()),
                write(decision.getPolicy()),
                write(decision.getReasons()),
                decision.getFailureReason(),
                decision.getDecidedBy(),
                decision.getDecidedAt());
    }

    static CreditDecision toDomain(CreditDecisionJpaEntity entity) {
        RiskAssessment risk = entity.getModelVersion() == null || entity.getProbabilityOfDefault() == null
                ? null
                : new RiskAssessment(entity.getProbabilityOfDefault(), entity.getRiskScore(), entity.getModelVersion());
        return CreditDecision.reconstitute(
                entity.getId(),
                entity.getCreditApplicationId(),
                entity.getStatus(),
                entity.getOutcome(),
                read(entity.getApplicationSnapshot(), new TypeReference<ApplicationSnapshot>() { }),
                ModelFeatures.fromFeatureMap(read(entity.getModelInput(), new TypeReference<Map<String, Number>>() { })),
                risk,
                entity.getExplanation() == null ? null : read(entity.getExplanation(), new TypeReference<Explanation>() { }),
                read(entity.getPolicy(), new TypeReference<DecisionPolicy>() { }),
                read(entity.getReasons(), new TypeReference<List<String>>() { }),
                entity.getFailureReason(),
                entity.getDecidedBy(),
                entity.getDecidedAt());
    }
}
