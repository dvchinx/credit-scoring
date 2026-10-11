package com.florez.backend.scoring.application;

import com.florez.backend.common.application.TransactionRunner;
import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.port.in.GetModelActivationsUseCase;
import com.florez.backend.scoring.domain.exception.CreditDecisionFailedException;
import com.florez.backend.scoring.domain.exception.NoActiveModelVersionException;
import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.ApplicationSnapshot;
import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.PolicyResult;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.port.in.EvaluateCreditApplicationUseCase;
import com.florez.backend.scoring.domain.port.in.ListCreditDecisionsUseCase;
import com.florez.backend.scoring.domain.port.out.CreditDecisionRepositoryPort;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CreditDecisionService implements EvaluateCreditApplicationUseCase, ListCreditDecisionsUseCase {

    private final CreditApplicationRepositoryPort applicationRepository;
    private final CreditDecisionRepositoryPort decisionRepository;
    private final ScoringModelPort scoringModel;
    private final GetModelActivationsUseCase modelActivations;
    private final DecisionPolicy policy;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public CreditDecisionService(
            CreditApplicationRepositoryPort applicationRepository,
            CreditDecisionRepositoryPort decisionRepository,
            ScoringModelPort scoringModel,
            GetModelActivationsUseCase modelActivations,
            DecisionPolicy policy,
            TransactionRunner transactionRunner,
            Clock clock) {
        this.applicationRepository = applicationRepository;
        this.decisionRepository = decisionRepository;
        this.scoringModel = scoringModel;
        this.modelActivations = modelActivations;
        this.policy = policy;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    @Override
    public CreditDecision evaluate(UUID creditApplicationId, String requestedBy) {
        CreditApplication application = findApplication(creditApplicationId);
        // La versión se fija aquí y se pide explícitamente al ML: la decisión registra la versión
        // que estaba activa en el momento de evaluar, nunca una inferida después.
        String modelVersion = modelActivations.current()
                .map(ModelActivation::modelVersion)
                .orElseThrow(NoActiveModelVersionException::new);
        Instant decidedAt = clock.instant();
        ApplicationSnapshot snapshot = ApplicationSnapshot.of(application);
        ModelFeatures features = ModelFeatures.from(application, LocalDate.ofInstant(decidedAt, clock.getZone()));

        // Las llamadas al modelo quedan fuera de la transacción: no se retiene una conexión
        // a la base de datos mientras se espera al servicio de ML.
        RiskAssessment risk;
        Explanation explanation;
        try {
            risk = scoringModel.score(modelVersion, features);
            explanation = scoringModel.explain(modelVersion, features);
        } catch (ScoringUnavailableException e) {
            throw recordFailure(application, snapshot, features, e.getMessage(), requestedBy, decidedAt);
        }
        if (!modelVersion.equals(risk.modelVersion()) || !modelVersion.equals(explanation.modelVersion())) {
            String reason = "se pidió el modelo " + modelVersion + " pero el score vino del modelo " + risk.modelVersion()
                    + " y la explicación del modelo " + explanation.modelVersion();
            throw recordFailure(application, snapshot, features, reason, requestedBy, decidedAt);
        }

        PolicyResult policyResult = policy.evaluate(features, risk);
        CreditDecision decision = CreditDecision.completed(
                application.getId(), snapshot, features, risk, explanation, policy, policyResult, requestedBy, decidedAt);

        return transactionRunner.inTransaction(() -> {
            CreditDecision saved = decisionRepository.save(decision);
            application.recordDecisionOutcome(policyResult.outcome().toApplicationStatus());
            applicationRepository.save(application);
            return saved;
        });
    }

    @Override
    public List<CreditDecision> listByApplication(UUID creditApplicationId) {
        findApplication(creditApplicationId);
        return decisionRepository.findByApplicationIdNewestFirst(creditApplicationId);
    }

    /** Registra el intento fallido (la solicitud no cambia de estado) y devuelve la excepción a lanzar. */
    private CreditDecisionFailedException recordFailure(
            CreditApplication application,
            ApplicationSnapshot snapshot,
            ModelFeatures features,
            String reason,
            String requestedBy,
            Instant decidedAt) {
        CreditDecision failed = decisionRepository.save(
                CreditDecision.failed(application.getId(), snapshot, features, policy, reason, requestedBy, decidedAt));
        return new CreditDecisionFailedException(failed.getId(), reason);
    }

    private CreditApplication findApplication(UUID creditApplicationId) {
        return applicationRepository.findByIdAndNotDeleted(creditApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una solicitud de crédito con id " + creditApplicationId));
    }
}
