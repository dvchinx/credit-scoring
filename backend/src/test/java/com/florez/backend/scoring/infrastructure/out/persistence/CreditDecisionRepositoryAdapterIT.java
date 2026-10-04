package com.florez.backend.scoring.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.TestcontainersConfig;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.scoring.ScoringTestData;
import com.florez.backend.scoring.domain.model.ApplicationSnapshot;
import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.PolicyResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfig.class)
class CreditDecisionRepositoryAdapterIT {

    @Autowired
    private CreditDecisionRepositoryAdapter adapter;

    @Autowired
    private CreditApplicationRepositoryPort applicationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CreditApplication persistedApplication(String documentId) {
        return applicationRepository.save(CreditApplication.createNew(
                "Grace Hopper", documentId, LocalDate.of(1988, 12, 9), new BigDecimal("4200.00"),
                new BigDecimal("9000.00"), 18, 7.0, new BigDecimal("350.00"), 2,
                new CreditHistory(0.42, 7, 1, 2, 1, 0), "analyst1"));
    }

    private CreditDecision completedDecision(CreditApplication application, Instant decidedAt) {
        ModelFeatures features = ModelFeatures.from(application, LocalDate.of(2026, 10, 4));
        return CreditDecision.completed(application.getId(), ApplicationSnapshot.of(application), features,
                ScoringTestData.risk(0.42, "20261004T120000Z"), ScoringTestData.explanation("20261004T120000Z"),
                ScoringTestData.POLICY, new PolicyResult(DecisionOutcome.MANUAL_REVIEW, List.of("entre umbrales")),
                "analyst1", decidedAt);
    }

    @Test
    void save_persisteYRecuperaLaDecisionCompletaConSusSnapshots() {
        CreditApplication application = persistedApplication("DEC-IT-1");
        Instant decidedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        CreditDecision original = completedDecision(application, decidedAt);

        CreditDecision saved = adapter.save(original);
        CreditDecision reloaded = adapter.findByApplicationIdNewestFirst(application.getId()).getFirst();

        assertThat(saved.getId()).isNotNull();
        assertThat(reloaded.getId()).isEqualTo(saved.getId());
        assertThat(reloaded.getOutcome()).isEqualTo(DecisionOutcome.MANUAL_REVIEW);
        assertThat(reloaded.getRiskAssessment()).isEqualTo(original.getRiskAssessment());
        assertThat(reloaded.getExplanation()).isEqualTo(original.getExplanation());
        assertThat(reloaded.getModelInput()).isEqualTo(original.getModelInput());
        assertThat(reloaded.getApplicationSnapshot()).isEqualTo(original.getApplicationSnapshot());
        assertThat(reloaded.getPolicy()).isEqualTo(ScoringTestData.POLICY);
        assertThat(reloaded.getReasons()).containsExactly("entre umbrales");
        assertThat(reloaded.getDecidedAt()).isEqualTo(decidedAt);
    }

    @Test
    void modelInput_seAlmacenaConLosNombresCanonicosDelModelo() {
        CreditApplication application = persistedApplication("DEC-IT-2");
        CreditDecision saved = adapter.save(completedDecision(application, Instant.now()));

        String debtRatio = jdbcTemplate.queryForObject(
                "SELECT model_input ->> 'debt_ratio' FROM credit_decisions WHERE id = ?", String.class, saved.getId());

        assertThat(Double.parseDouble(debtRatio)).isEqualTo(saved.getModelInput().debtRatio());
    }

    @Test
    void findByApplicationIdNewestFirst_ordenaDeLaMasRecienteALaMasAntigua() {
        CreditApplication application = persistedApplication("DEC-IT-3");
        Instant first = Instant.parse("2026-10-01T10:00:00Z");
        Instant second = Instant.parse("2026-10-02T10:00:00Z");
        adapter.save(completedDecision(application, first));
        adapter.save(completedDecision(application, second));

        assertThat(adapter.findByApplicationIdNewestFirst(application.getId()))
                .extracting(CreditDecision::getDecidedAt)
                .containsExactly(second, first);
    }

    @Test
    void laTablaEsAppendOnly_laBaseDeDatosRechazaUpdateYDelete() {
        CreditApplication application = persistedApplication("DEC-IT-4");
        CreditDecision saved = adapter.save(completedDecision(application, Instant.now()));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE credit_decisions SET outcome = 'APPROVED' WHERE id = ?", saved.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM credit_decisions WHERE id = ?", saved.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
    }

    @Test
    void laBaseDeDatosRechazaUnaDecisionCompletadaSinExplicacion() {
        CreditApplication application = persistedApplication("DEC-IT-5");

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO credit_decisions (id, credit_application_id, status, outcome, probability_of_default,
                    risk_score, model_version, application_snapshot, model_input, explanation, policy, reasons,
                    decided_by, decided_at)
                VALUES (gen_random_uuid(), ?, 'COMPLETED', 'APPROVED', 0.1, 795, 'v1', '{}', '{}', NULL, '{}', '[]',
                    'analyst1', now())
                """, application.getId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_credit_decisions_completed_is_explained");
    }
}
