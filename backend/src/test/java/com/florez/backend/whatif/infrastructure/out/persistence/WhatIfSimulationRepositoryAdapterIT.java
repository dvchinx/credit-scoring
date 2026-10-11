package com.florez.backend.whatif.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.TestcontainersConfig;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.modelregistry.ModelRegistryTestData;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import com.florez.backend.scoring.ScoringTestData;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.whatif.WhatIfTestData;
import com.florez.backend.whatif.domain.model.ApplicationChanges;
import com.florez.backend.whatif.domain.model.ModelEvaluation;
import com.florez.backend.whatif.domain.model.ScenarioResult;
import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfig.class)
class WhatIfSimulationRepositoryAdapterIT {

    private static final String MODEL_VERSION = "whatif-persist-v1";

    @Autowired
    private WhatIfSimulationRepositoryAdapter adapter;

    @Autowired
    private CreditApplicationRepositoryPort applications;

    @Autowired
    private ModelVersionRepositoryPort modelVersions;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void registerModelVersion() {
        if (!modelVersions.exists(MODEL_VERSION)) {
            modelVersions.save(new ModelVersion(ModelRegistryTestData.artifact(MODEL_VERSION), "admin", Instant.now()));
        }
    }

    private WhatIfSimulation simulation(String documentId) {
        CreditApplication application = applications.save(CreditApplication.createNew(
                "Grace Hopper", documentId, LocalDate.of(1988, 12, 9), new BigDecimal("4200.00"),
                new BigDecimal("9000.00"), 18, 7.0, new BigDecimal("350.00"), 2,
                new CreditHistory(0.42, 7, 1, 2, 1, 0), "analyst1"));
        ApplicationChanges changes = new ApplicationChanges(
                new BigDecimal("5000.00"), null, null, null, null, 0.10, null, null, null, null, null);
        LocalDate today = LocalDate.of(2026, 10, 11);
        ModelFeatures baseline = ModelFeatures.from(application, today);
        ModelFeatures scenario = ModelFeatures.from(changes.applyTo(application), today);
        ModelEvaluation baselineEval = WhatIfTestData.evaluation(baseline, 0.45, MODEL_VERSION);
        ModelEvaluation scenarioEval = WhatIfTestData.evaluation(scenario, 0.20, MODEL_VERSION);
        return WhatIfSimulation.create(
                application.getId(),
                MODEL_VERSION,
                changes.describe(application),
                new ScenarioResult(baseline, baselineEval.risk(), baselineEval.explanation(),
                        ScoringTestData.POLICY.evaluate(baseline, baselineEval.risk())),
                new ScenarioResult(scenario, scenarioEval.risk(), scenarioEval.explanation(),
                        ScoringTestData.POLICY.evaluate(scenario, scenarioEval.risk())),
                ScoringTestData.POLICY,
                "analyst1",
                Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void save_persisteYRecuperaLaSimulacionCompleta() {
        WhatIfSimulation original = simulation("WHATIF-IT-1");

        WhatIfSimulation saved = adapter.save(original);
        WhatIfSimulation reloaded = adapter.findByApplicationIdNewestFirst(original.getCreditApplicationId()).getFirst();

        assertThat(saved.getId()).isNotNull();
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getChanges()).isEqualTo(original.getChanges());
        assertThat(reloaded.getBaseline()).isEqualTo(original.getBaseline());
        assertThat(reloaded.getScenario()).isEqualTo(original.getScenario());
        assertThat(reloaded.getPolicy()).isEqualTo(original.getPolicy());
        assertThat(reloaded.decisionChanged()).isTrue();
        assertThat(reloaded.getSimulatedAt()).isEqualTo(original.getSimulatedAt());
    }

    @Test
    void lasSimulacionesSonAppendOnly() {
        WhatIfSimulation saved = adapter.save(simulation("WHATIF-IT-2"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE what_if_simulations SET scenario_outcome = 'REJECTED' WHERE id = ?", saved.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("what_if_simulations es append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM what_if_simulations WHERE id = ?", saved.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void lasColumnasDeResultadoPermitenAuditarSimulacionesQueCambiaronLaDecision() {
        WhatIfSimulation saved = adapter.save(simulation("WHATIF-IT-3"));

        Integer flipped = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM what_if_simulations WHERE id = ? AND baseline_outcome <> scenario_outcome",
                Integer.class, saved.getId());

        assertThat(flipped).isEqualTo(1);
    }
}
