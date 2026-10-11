package com.florez.backend.whatif.infrastructure.out.persistence;

import static com.florez.backend.common.infrastructure.persistence.JsonColumnCodec.read;
import static com.florez.backend.common.infrastructure.persistence.JsonColumnCodec.write;

import com.fasterxml.jackson.core.type.TypeReference;
import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.PolicyResult;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.whatif.domain.model.ScenarioResult;
import com.florez.backend.whatif.domain.model.VariableChange;
import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import java.util.List;
import java.util.Map;

/**
 * Cada escenario se guarda como un documento JSONB autocontenido; la entrada del modelo usa los
 * nombres canónicos, igual que en {@code credit_decisions}.
 */
final class WhatIfSimulationMapper {

    private WhatIfSimulationMapper() {
    }

    static WhatIfSimulationJpaEntity toNewEntity(WhatIfSimulation simulation) {
        return new WhatIfSimulationJpaEntity(
                simulation.getCreditApplicationId(),
                simulation.getModelVersion(),
                write(simulation.getChanges()),
                write(ScenarioDocument.from(simulation.getBaseline())),
                write(ScenarioDocument.from(simulation.getScenario())),
                simulation.getBaseline().outcome(),
                simulation.getScenario().outcome(),
                write(simulation.getPolicy()),
                simulation.getRequestedBy(),
                simulation.getSimulatedAt());
    }

    static WhatIfSimulation toDomain(WhatIfSimulationJpaEntity entity) {
        return WhatIfSimulation.reconstitute(
                entity.getId(),
                entity.getCreditApplicationId(),
                entity.getModelVersion(),
                read(entity.getChanges(), new TypeReference<List<VariableChange>>() { }),
                read(entity.getBaseline(), new TypeReference<ScenarioDocument>() { }).toDomain(entity.getModelVersion()),
                read(entity.getScenario(), new TypeReference<ScenarioDocument>() { }).toDomain(entity.getModelVersion()),
                read(entity.getPolicy(), new TypeReference<DecisionPolicy>() { }),
                entity.getRequestedBy(),
                entity.getSimulatedAt());
    }

    record ScenarioDocument(
            Map<String, Number> modelInput,
            double probabilityOfDefault,
            int riskScore,
            DecisionOutcome outcome,
            List<String> reasons,
            Explanation explanation) {

        static ScenarioDocument from(ScenarioResult result) {
            return new ScenarioDocument(
                    result.modelInput().asFeatureMap(),
                    result.risk().probabilityOfDefault(),
                    result.risk().riskScore(),
                    result.outcome(),
                    result.policyResult().reasons(),
                    result.explanation());
        }

        ScenarioResult toDomain(String modelVersion) {
            return new ScenarioResult(
                    ModelFeatures.fromFeatureMap(modelInput),
                    new RiskAssessment(probabilityOfDefault, riskScore, modelVersion),
                    explanation,
                    new PolicyResult(outcome, reasons));
        }
    }
}
