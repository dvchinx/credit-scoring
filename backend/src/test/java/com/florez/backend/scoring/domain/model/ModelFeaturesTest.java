package com.florez.backend.scoring.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.florez.backend.scoring.ScoringTestData;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModelFeaturesTest {

    @Test
    void from_calculaEdadALaFechaDeEvaluacion() {
        var application = ScoringTestData.pendingApplication(UUID.randomUUID());

        assertThat(ModelFeatures.from(application, LocalDate.of(2026, 6, 14)).age()).isEqualTo(35);
        assertThat(ModelFeatures.from(application, LocalDate.of(2026, 6, 15)).age()).isEqualTo(36);
    }

    @Test
    void from_incluyeLaCuotaDelCreditoSolicitadoEnElEndeudamiento() {
        var application = ScoringTestData.pendingApplication(UUID.randomUUID());

        ModelFeatures features = ModelFeatures.from(application, LocalDate.of(2026, 10, 4));

        // (300 de deuda existente + 12000 / 24 de cuota nueva) / 3000 de ingreso
        assertThat(features.debtRatio()).isCloseTo(800.0 / 3000.0, within(1e-12));
    }

    @Test
    void from_tomaElHistorialCrediticioSinTransformarlo() {
        var application = ScoringTestData.pendingApplication(UUID.randomUUID());

        ModelFeatures features = ModelFeatures.from(application, LocalDate.of(2026, 10, 4));

        assertThat(features.revolvingUtilizationOfUnsecuredLines()).isEqualTo(0.35);
        assertThat(features.numberOfOpenCreditLinesAndLoans()).isEqualTo(6);
        assertThat(features.numberRealEstateLoansOrLines()).isEqualTo(1);
        assertThat(features.numberOfTime30To59DaysPastDueNotWorse()).isEqualTo(1);
        assertThat(features.numberOfTime60To89DaysPastDueNotWorse()).isZero();
        assertThat(features.numberOfTimes90DaysLate()).isZero();
        assertThat(features.monthlyIncome()).isEqualTo(3000.0);
        assertThat(features.numberOfDependents()).isEqualTo(1);
    }

    @Test
    void asFeatureMap_usaLosNombresCanonicosDelModeloEnOrdenDeEntrenamiento() {
        var features = ModelFeatures.from(ScoringTestData.pendingApplication(UUID.randomUUID()), LocalDate.of(2026, 10, 4));

        assertThat(features.asFeatureMap().keySet()).containsExactly(
                "revolving_utilization_of_unsecured_lines",
                "age",
                "number_of_time_30_59_days_past_due_not_worse",
                "debt_ratio",
                "monthly_income",
                "number_of_open_credit_lines_and_loans",
                "number_of_times_90_days_late",
                "number_real_estate_loans_or_lines",
                "number_of_time_60_89_days_past_due_not_worse",
                "number_of_dependents");
    }
}
