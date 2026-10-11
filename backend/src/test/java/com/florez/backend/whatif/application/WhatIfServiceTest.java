package com.florez.backend.whatif.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.ApplicationStatus;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.port.in.GetModelActivationsUseCase;
import com.florez.backend.scoring.ScoringTestData;
import com.florez.backend.scoring.domain.exception.NoActiveModelVersionException;
import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.whatif.WhatIfTestData;
import com.florez.backend.whatif.domain.model.ApplicationChanges;
import com.florez.backend.whatif.domain.model.FeatureImpact;
import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort.Comparison;
import com.florez.backend.whatif.domain.port.out.WhatIfSimulationRepositoryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatIfServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-11T15:00:00Z");

    @Mock
    private CreditApplicationRepositoryPort applicationRepository;

    @Mock
    private WhatIfSimulationRepositoryPort simulationRepository;

    @Mock
    private WhatIfModelPort model;

    @Mock
    private GetModelActivationsUseCase modelActivations;

    private WhatIfService service;
    private final UUID applicationId = UUID.randomUUID();
    private CreditApplication application;

    @BeforeEach
    void setUp() {
        service = new WhatIfService(applicationRepository, simulationRepository, model, modelActivations,
                ScoringTestData.POLICY, Clock.fixed(NOW, ZoneOffset.UTC));
        application = ScoringTestData.pendingApplication(applicationId);
        lenient().when(applicationRepository.findByIdAndNotDeleted(applicationId)).thenReturn(Optional.of(application));
        lenient().when(modelActivations.current())
                .thenReturn(Optional.of(new ModelActivation(1L, "v1", "inicial", "admin", NOW.minusSeconds(60))));
        lenient().when(simulationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** El modelo simulado devuelve pd distinta para cada escenario, con explicaciones coherentes con su entrada. */
    private void modelAnswers(double baselinePd, double scenarioPd) {
        when(model.compare(eq("v1"), any(), any())).thenAnswer(invocation -> new Comparison(
                WhatIfTestData.evaluation(invocation.getArgument(1), baselinePd, "v1"),
                WhatIfTestData.evaluation(invocation.getArgument(2), scenarioPd, "v1")));
    }

    private static ApplicationChanges utilization(double value) {
        return new ApplicationChanges(null, null, null, null, null, value, null, null, null, null, null);
    }

    @Test
    void simulate_comparaAmbosEscenariosConLaVersionActivaYLaPolitica() {
        modelAnswers(0.45, 0.20);

        WhatIfSimulation simulation = service.simulate(applicationId, utilization(0.05), "analyst1");

        assertThat(simulation.getModelVersion()).isEqualTo("v1");
        assertThat(simulation.getBaseline().outcome()).isEqualTo(DecisionOutcome.MANUAL_REVIEW);
        assertThat(simulation.getScenario().outcome()).isEqualTo(DecisionOutcome.APPROVED);
        assertThat(simulation.decisionChanged()).isTrue();
        assertThat(simulation.probabilityOfDefaultDelta()).isCloseTo(-0.25, within(1e-9));
        assertThat(simulation.getRequestedBy()).isEqualTo("analyst1");
        assertThat(simulation.getSimulatedAt()).isEqualTo(NOW);
    }

    @Test
    void simulate_soloCambiaLaContribucionDeLaVariableSimulada() {
        modelAnswers(0.45, 0.20);

        WhatIfSimulation simulation = service.simulate(applicationId, utilization(0.05), "analyst1");

        FeatureImpact top = simulation.featureImpacts().getFirst();
        assertThat(top.feature()).isEqualTo("revolving_utilization_of_unsecured_lines");
        assertThat(top.baselineValue()).isEqualTo(0.35);
        assertThat(top.scenarioValue()).isEqualTo(0.05);
        assertThat(top.shapDelta()).isCloseTo(1.2 * (0.05 - 0.35), within(1e-9));
        assertThat(simulation.featureImpacts()).filteredOn(FeatureImpact::changed).hasSize(1);
    }

    @Test
    void simulate_cambiarElMontoRecalculaElEndeudamientoComoEnUnaDecisionReal() {
        modelAnswers(0.20, 0.20);
        ApplicationChanges biggerLoan = new ApplicationChanges(
                null, new BigDecimal("36000.00"), null, null, null, null, null, null, null, null, null);

        WhatIfSimulation simulation = service.simulate(applicationId, biggerLoan, "analyst1");

        // (300 + 36000 / 24) / 3000 = 60% > 50%: la regla dura rechaza aunque el riesgo del modelo no cambie
        assertThat(simulation.getScenario().modelInput().debtRatio()).isCloseTo(1800.0 / 3000.0, within(1e-12));
        assertThat(simulation.getBaseline().outcome()).isEqualTo(DecisionOutcome.APPROVED);
        assertThat(simulation.getScenario().outcome()).isEqualTo(DecisionOutcome.REJECTED);
        assertThat(simulation.getScenario().policyResult().reasons().getFirst()).contains("Endeudamiento");
    }

    @Test
    void simulate_enviaAlModeloLaEntradaRealYLaSimulada() {
        modelAnswers(0.45, 0.20);

        service.simulate(applicationId, utilization(0.05), "analyst1");

        ArgumentCaptor<ModelFeatures> baseline = ArgumentCaptor.forClass(ModelFeatures.class);
        ArgumentCaptor<ModelFeatures> scenario = ArgumentCaptor.forClass(ModelFeatures.class);
        verify(model).compare(eq("v1"), baseline.capture(), scenario.capture());
        assertThat(baseline.getValue().revolvingUtilizationOfUnsecuredLines()).isEqualTo(0.35);
        assertThat(scenario.getValue().revolvingUtilizationOfUnsecuredLines()).isEqualTo(0.05);
        assertThat(scenario.getValue().age()).isEqualTo(baseline.getValue().age());
    }

    @Test
    void simulate_nuncaModificaLaSolicitudNiCreaDecisiones() {
        modelAnswers(0.45, 0.20);

        service.simulate(applicationId, utilization(0.05), "analyst1");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        verify(applicationRepository, never()).save(any());
        verify(simulationRepository).save(any());
    }

    @Test
    void simulate_sinVersionActiva_noLlamaAlModelo() {
        when(modelActivations.current()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulate(applicationId, utilization(0.05), "analyst1"))
                .isInstanceOf(NoActiveModelVersionException.class);
        verify(model, never()).compare(anyString(), any(), any());
    }

    @Test
    void simulate_siElMlRespondeConOtraVersion_fallaSinRegistrar() {
        when(model.compare(eq("v1"), any(), any())).thenAnswer(invocation -> new Comparison(
                WhatIfTestData.evaluation(invocation.getArgument(1), 0.4, "v2"),
                WhatIfTestData.evaluation(invocation.getArgument(2), 0.2, "v2")));

        assertThatThrownBy(() -> service.simulate(applicationId, utilization(0.05), "analyst1"))
                .isInstanceOf(ScoringUnavailableException.class)
                .hasMessageContaining("se pidió el modelo v1");
        verify(simulationRepository, never()).save(any());
    }

    @Test
    void simulate_solicitudInexistente_lanzaNotFound() {
        UUID unknown = UUID.randomUUID();
        when(applicationRepository.findByIdAndNotDeleted(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulate(unknown, utilization(0.05), "analyst1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
