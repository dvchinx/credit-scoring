package com.florez.backend.whatif.domain.model;

import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.PolicyResult;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import java.util.Objects;

/** Resultado de evaluar un escenario: entrada del modelo, score, explicación y decisión que tomaría la política. */
public record ScenarioResult(ModelFeatures modelInput, RiskAssessment risk, Explanation explanation, PolicyResult policyResult) {

    public ScenarioResult {
        Objects.requireNonNull(modelInput, "modelInput");
        Objects.requireNonNull(risk, "risk");
        Objects.requireNonNull(explanation, "explanation");
        Objects.requireNonNull(policyResult, "policyResult");
    }

    public DecisionOutcome outcome() {
        return policyResult.outcome();
    }
}
