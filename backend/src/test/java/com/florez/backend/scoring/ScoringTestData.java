package com.florez.backend.scoring;

import com.florez.backend.creditapplication.domain.model.ApplicationStatus;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.FeatureContribution;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.model.RiskDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ScoringTestData {

    public static final DecisionPolicy POLICY = new DecisionPolicy(0.30, 0.60, 0.50, 18);

    private ScoringTestData() {
    }

    /** Ingreso 3000, deuda 300, pide 12000 a 24 meses (cuota 500): debt_ratio = 800 / 3000. */
    public static CreditApplication pendingApplication(UUID id) {
        return CreditApplication.reconstitute(
                id,
                "Ada Lovelace",
                "DOC-" + id,
                LocalDate.of(1990, 6, 15),
                new BigDecimal("3000.00"),
                new BigDecimal("12000.00"),
                24,
                4.5,
                new BigDecimal("300.00"),
                1,
                new CreditHistory(0.35, 6, 1, 1, 0, 0),
                ApplicationStatus.PENDING,
                "analyst1",
                Instant.parse("2026-10-01T10:00:00Z"),
                Instant.parse("2026-10-01T10:00:00Z"),
                null);
    }

    public static RiskAssessment risk(double probabilityOfDefault, String modelVersion) {
        return new RiskAssessment(probabilityOfDefault, (int) Math.round(850 - probabilityOfDefault * 550), modelVersion);
    }

    public static Explanation explanation(String modelVersion) {
        return new Explanation(modelVersion, -0.40, -0.95, List.of(
                new FeatureContribution("number_of_time_30_59_days_past_due_not_worse", 1, 0.35, RiskDirection.INCREASES_RISK),
                new FeatureContribution("age", 36, -0.60, RiskDirection.DECREASES_RISK),
                new FeatureContribution("revolving_utilization_of_unsecured_lines", 0.35, -0.30, RiskDirection.DECREASES_RISK)));
    }
}
