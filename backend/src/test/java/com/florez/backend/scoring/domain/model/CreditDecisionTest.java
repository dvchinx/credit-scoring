package com.florez.backend.scoring.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.scoring.ScoringTestData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreditDecisionTest {

    private final UUID applicationId = UUID.randomUUID();
    private final ApplicationSnapshot snapshot = ApplicationSnapshot.of(ScoringTestData.pendingApplication(applicationId));
    private final ModelFeatures features =
            ModelFeatures.from(ScoringTestData.pendingApplication(applicationId), LocalDate.of(2026, 10, 4));
    private final PolicyResult approved = new PolicyResult(DecisionOutcome.APPROVED, List.of("ok"));
    private final Instant now = Instant.parse("2026-10-04T15:00:00Z");

    @Test
    void completed_sinExplicacion_noPuedeExistir() {
        assertThatThrownBy(() -> CreditDecision.completed(applicationId, snapshot, features,
                ScoringTestData.risk(0.1, "v1"), null, ScoringTestData.POLICY, approved, "analyst1", now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("explicación");
    }

    @Test
    void completed_conScoreYExplicacionDeVersionesDistintas_noPuedeExistir() {
        assertThatThrownBy(() -> CreditDecision.completed(applicationId, snapshot, features,
                ScoringTestData.risk(0.1, "v1"), ScoringTestData.explanation("v2"), ScoringTestData.POLICY, approved,
                "analyst1", now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("misma versión");
    }

    @Test
    void failed_sinMotivo_noPuedeExistir() {
        assertThatThrownBy(() -> CreditDecision.failed(
                applicationId, snapshot, features, ScoringTestData.POLICY, null, "analyst1", now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failed_noTieneResultadoNiScore() {
        CreditDecision failed = CreditDecision.failed(
                applicationId, snapshot, features, ScoringTestData.POLICY, "ML caído", "analyst1", now);

        assertThat(failed.getStatus()).isEqualTo(DecisionStatus.FAILED);
        assertThat(failed.getOutcome()).isNull();
        assertThat(failed.getRiskAssessment()).isNull();
        assertThat(failed.getExplanation()).isNull();
    }
}
