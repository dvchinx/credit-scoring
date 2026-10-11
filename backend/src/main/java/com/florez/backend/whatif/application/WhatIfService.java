package com.florez.backend.whatif.application;

import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.port.in.GetModelActivationsUseCase;
import com.florez.backend.scoring.domain.exception.NoActiveModelVersionException;
import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.whatif.domain.model.ApplicationChanges;
import com.florez.backend.whatif.domain.model.ModelEvaluation;
import com.florez.backend.whatif.domain.model.ScenarioResult;
import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import com.florez.backend.whatif.domain.port.in.ListWhatIfSimulationsUseCase;
import com.florez.backend.whatif.domain.port.in.SimulateWhatIfUseCase;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort.Comparison;
import com.florez.backend.whatif.domain.port.out.WhatIfSimulationRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class WhatIfService implements SimulateWhatIfUseCase, ListWhatIfSimulationsUseCase {

    private final CreditApplicationRepositoryPort applicationRepository;
    private final WhatIfSimulationRepositoryPort simulationRepository;
    private final WhatIfModelPort model;
    private final GetModelActivationsUseCase modelActivations;
    private final DecisionPolicy policy;
    private final Clock clock;

    public WhatIfService(
            CreditApplicationRepositoryPort applicationRepository,
            WhatIfSimulationRepositoryPort simulationRepository,
            WhatIfModelPort model,
            GetModelActivationsUseCase modelActivations,
            DecisionPolicy policy,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.simulationRepository = simulationRepository;
        this.model = model;
        this.modelActivations = modelActivations;
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public WhatIfSimulation simulate(UUID creditApplicationId, ApplicationChanges changes, String requestedBy) {
        CreditApplication application = findApplication(creditApplicationId);
        String modelVersion = modelActivations.current()
                .map(ModelActivation::modelVersion)
                .orElseThrow(NoActiveModelVersionException::new);
        Instant simulatedAt = clock.instant();
        LocalDate evaluationDate = LocalDate.ofInstant(simulatedAt, clock.getZone());

        // Ambos escenarios pasan por la misma derivación de variables que una evaluación real
        // (p. ej. debt_ratio con la cuota nueva), así que las reglas duras también se simulan.
        ModelFeatures baselineFeatures = ModelFeatures.from(application, evaluationDate);
        ModelFeatures scenarioFeatures = ModelFeatures.from(changes.applyTo(application), evaluationDate);

        Comparison comparison = model.compare(modelVersion, baselineFeatures, scenarioFeatures);
        requireVersion(modelVersion, comparison.baseline());
        requireVersion(modelVersion, comparison.scenario());

        WhatIfSimulation simulation = WhatIfSimulation.create(
                application.getId(),
                modelVersion,
                changes.describe(application),
                evaluate(baselineFeatures, comparison.baseline()),
                evaluate(scenarioFeatures, comparison.scenario()),
                policy,
                requestedBy,
                simulatedAt);
        return simulationRepository.save(simulation);
    }

    @Override
    public List<WhatIfSimulation> listByApplication(UUID creditApplicationId) {
        findApplication(creditApplicationId);
        return simulationRepository.findByApplicationIdNewestFirst(creditApplicationId);
    }

    private ScenarioResult evaluate(ModelFeatures features, ModelEvaluation evaluation) {
        return new ScenarioResult(features, evaluation.risk(), evaluation.explanation(), policy.evaluate(features, evaluation.risk()));
    }

    private static void requireVersion(String expected, ModelEvaluation evaluation) {
        if (!expected.equals(evaluation.risk().modelVersion()) || !expected.equals(evaluation.explanation().modelVersion())) {
            throw new ScoringUnavailableException("se pidió el modelo " + expected + " pero el servicio de ML respondió con "
                    + evaluation.risk().modelVersion());
        }
    }

    private CreditApplication findApplication(UUID creditApplicationId) {
        return applicationRepository.findByIdAndNotDeleted(creditApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una solicitud de crédito con id " + creditApplicationId));
    }
}
