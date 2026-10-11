package com.florez.backend.whatif.domain.model;

import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.FeatureContribution;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simulación "qué pasaría si" sobre una solicitud. Ambos escenarios se evalúan con la misma versión
 * de modelo y la misma política, de modo que cualquier diferencia se debe solo a los cambios
 * simulados. Es un registro de auditoría: nunca crea decisiones ni cambia el estado de la solicitud.
 */
public final class WhatIfSimulation {

    private final UUID id;
    private final UUID creditApplicationId;
    private final String modelVersion;
    private final List<VariableChange> changes;
    private final ScenarioResult baseline;
    private final ScenarioResult scenario;
    private final DecisionPolicy policy;
    private final String requestedBy;
    private final Instant simulatedAt;

    private WhatIfSimulation(
            UUID id,
            UUID creditApplicationId,
            String modelVersion,
            List<VariableChange> changes,
            ScenarioResult baseline,
            ScenarioResult scenario,
            DecisionPolicy policy,
            String requestedBy,
            Instant simulatedAt) {
        this.id = id;
        this.creditApplicationId = Objects.requireNonNull(creditApplicationId, "creditApplicationId");
        this.modelVersion = Objects.requireNonNull(modelVersion, "modelVersion");
        this.changes = List.copyOf(changes);
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.scenario = Objects.requireNonNull(scenario, "scenario");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
        this.simulatedAt = Objects.requireNonNull(simulatedAt, "simulatedAt");
        if (this.changes.isEmpty()) {
            throw new IllegalArgumentException("Una simulación debe incluir al menos un cambio");
        }
        for (ScenarioResult result : List.of(baseline, scenario)) {
            if (!modelVersion.equals(result.risk().modelVersion()) || !modelVersion.equals(result.explanation().modelVersion())) {
                throw new IllegalArgumentException("Ambos escenarios deben evaluarse con el modelo " + modelVersion);
            }
        }
    }

    public static WhatIfSimulation create(
            UUID creditApplicationId,
            String modelVersion,
            List<VariableChange> changes,
            ScenarioResult baseline,
            ScenarioResult scenario,
            DecisionPolicy policy,
            String requestedBy,
            Instant simulatedAt) {
        return new WhatIfSimulation(
                null, creditApplicationId, modelVersion, changes, baseline, scenario, policy, requestedBy, simulatedAt);
    }

    public static WhatIfSimulation reconstitute(
            UUID id,
            UUID creditApplicationId,
            String modelVersion,
            List<VariableChange> changes,
            ScenarioResult baseline,
            ScenarioResult scenario,
            DecisionPolicy policy,
            String requestedBy,
            Instant simulatedAt) {
        return new WhatIfSimulation(
                id, creditApplicationId, modelVersion, changes, baseline, scenario, policy, requestedBy, simulatedAt);
    }

    public boolean decisionChanged() {
        return baseline.outcome() != scenario.outcome();
    }

    public double probabilityOfDefaultDelta() {
        return scenario.risk().probabilityOfDefault() - baseline.risk().probabilityOfDefault();
    }

    public int riskScoreDelta() {
        return scenario.risk().riskScore() - baseline.risk().riskScore();
    }

    /** Variables del modelo con su valor y contribución en ambos escenarios, de mayor a menor impacto. */
    public List<FeatureImpact> featureImpacts() {
        Map<String, FeatureContribution> simulated = scenario.explanation().contributions().stream()
                .collect(Collectors.toMap(FeatureContribution::feature, Function.identity()));
        return baseline.explanation().contributions().stream()
                .filter(real -> simulated.containsKey(real.feature()))
                .map(real -> {
                    FeatureContribution sim = simulated.get(real.feature());
                    return new FeatureImpact(real.feature(), real.value(), sim.value(), real.shapValue(), sim.shapValue());
                })
                .sorted(Comparator.comparingDouble((FeatureImpact impact) -> Math.abs(impact.shapDelta())).reversed())
                .toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreditApplicationId() {
        return creditApplicationId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public List<VariableChange> getChanges() {
        return changes;
    }

    public ScenarioResult getBaseline() {
        return baseline;
    }

    public ScenarioResult getScenario() {
        return scenario;
    }

    public DecisionPolicy getPolicy() {
        return policy;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getSimulatedAt() {
        return simulatedAt;
    }
}
