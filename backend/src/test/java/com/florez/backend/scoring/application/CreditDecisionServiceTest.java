package com.florez.backend.scoring.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.florez.backend.common.application.TransactionRunner;
import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.ApplicationStatus;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.scoring.ScoringTestData;
import com.florez.backend.scoring.domain.exception.CreditDecisionFailedException;
import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.DecisionStatus;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.port.out.CreditDecisionRepositoryPort;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditDecisionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-04T15:00:00Z");

    @Mock
    private CreditApplicationRepositoryPort applicationRepository;

    @Mock
    private CreditDecisionRepositoryPort decisionRepository;

    @Mock
    private ScoringModelPort scoringModel;

    private CreditDecisionService service;

    private final UUID applicationId = UUID.randomUUID();
    private CreditApplication application;

    @BeforeEach
    void setUp() {
        TransactionRunner inlineTransactions = new TransactionRunner() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return work.get();
            }
        };
        service = new CreditDecisionService(applicationRepository, decisionRepository, scoringModel,
                ScoringTestData.POLICY, inlineTransactions, Clock.fixed(NOW, ZoneOffset.UTC));
        application = ScoringTestData.pendingApplication(applicationId);
        lenient().when(applicationRepository.findByIdAndNotDeleted(applicationId)).thenReturn(Optional.of(application));
    }

    @Test
    void evaluate_conModeloDisponible_persisteDecisionExplicadaYActualizaLaSolicitud() {
        when(scoringModel.score(any())).thenReturn(ScoringTestData.risk(0.12, "v1"));
        when(scoringModel.explain(any())).thenReturn(ScoringTestData.explanation("v1"));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreditDecision decision = service.evaluate(applicationId, "analyst1");

        assertThat(decision.getStatus()).isEqualTo(DecisionStatus.COMPLETED);
        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.APPROVED);
        assertThat(decision.getRiskAssessment().modelVersion()).isEqualTo("v1");
        assertThat(decision.getExplanation().contributions()).isNotEmpty();
        assertThat(decision.getPolicy()).isEqualTo(ScoringTestData.POLICY);
        assertThat(decision.getDecidedBy()).isEqualTo("analyst1");
        assertThat(decision.getDecidedAt()).isEqualTo(NOW);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        verify(applicationRepository).save(application);
    }

    @Test
    void evaluate_enviaAlModeloLasFeaturesDerivadasALaFechaDeEvaluacion() {
        when(scoringModel.score(any())).thenReturn(ScoringTestData.risk(0.12, "v1"));
        when(scoringModel.explain(any())).thenReturn(ScoringTestData.explanation("v1"));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.evaluate(applicationId, "analyst1");

        ArgumentCaptor<ModelFeatures> sent = ArgumentCaptor.forClass(ModelFeatures.class);
        verify(scoringModel).score(sent.capture());
        assertThat(sent.getValue().age()).isEqualTo(36);
        verify(scoringModel).explain(sent.getValue());
    }

    @Test
    void evaluate_conServicioDeMlCaido_registraIntentoFallidoSinCambiarLaSolicitud() {
        when(scoringModel.score(any())).thenThrow(new ScoringUnavailableException("timeout"));
        UUID failedDecisionId = UUID.randomUUID();
        when(decisionRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), failedDecisionId));

        assertThatThrownBy(() -> service.evaluate(applicationId, "analyst1"))
                .isInstanceOf(CreditDecisionFailedException.class)
                .hasMessageContaining("timeout")
                .hasMessageContaining(failedDecisionId.toString());

        ArgumentCaptor<CreditDecision> saved = ArgumentCaptor.forClass(CreditDecision.class);
        verify(decisionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DecisionStatus.FAILED);
        assertThat(saved.getValue().getFailureReason()).isEqualTo("timeout");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void evaluate_siLaExplicacionFalla_noGuardaUnaDecisionFinal() {
        when(scoringModel.score(any())).thenReturn(ScoringTestData.risk(0.12, "v1"));
        when(scoringModel.explain(any())).thenThrow(new ScoringUnavailableException("explain caído"));
        when(decisionRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        assertThatThrownBy(() -> service.evaluate(applicationId, "analyst1"))
                .isInstanceOf(CreditDecisionFailedException.class);

        ArgumentCaptor<CreditDecision> saved = ArgumentCaptor.forClass(CreditDecision.class);
        verify(decisionRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DecisionStatus.FAILED);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void evaluate_conScoreYExplicacionDeVersionesDistintas_registraFallo() {
        when(scoringModel.score(any())).thenReturn(ScoringTestData.risk(0.12, "v1"));
        when(scoringModel.explain(any())).thenReturn(ScoringTestData.explanation("v2"));
        when(decisionRepository.save(any())).thenAnswer(invocation -> withId(invocation.getArgument(0), UUID.randomUUID()));

        assertThatThrownBy(() -> service.evaluate(applicationId, "analyst1"))
                .isInstanceOf(CreditDecisionFailedException.class)
                .hasMessageContaining("versiones distintas");

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void evaluate_conSolicitudInexistente_lanzaResourceNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(applicationRepository.findByIdAndNotDeleted(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate(unknownId, "analyst1")).isInstanceOf(ResourceNotFoundException.class);
        verify(scoringModel, never()).score(any());
    }

    private static CreditDecision withId(CreditDecision decision, UUID id) {
        return CreditDecision.reconstitute(id, decision.getCreditApplicationId(), decision.getStatus(),
                decision.getOutcome(), decision.getApplicationSnapshot(), decision.getModelInput(),
                decision.getRiskAssessment(), decision.getExplanation(), decision.getPolicy(), decision.getReasons(),
                decision.getFailureReason(), decision.getDecidedBy(), decision.getDecidedAt());
    }
}
