package com.florez.backend.whatif.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.scoring.ScoringTestData;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationChangesTest {

    private final CreditApplication application = ScoringTestData.pendingApplication(UUID.randomUUID());

    private static ApplicationChanges changes(BigDecimal income, Double revolvingUtilization) {
        return new ApplicationChanges(income, null, null, null, null, revolvingUtilization, null, null, null, null, null);
    }

    @Test
    void applyTo_cambiaSoloLasVariablesIndicadasYNoTocaLaSolicitudReal() {
        CreditApplication simulated = changes(new BigDecimal("4500.00"), 0.10).applyTo(application);

        assertThat(simulated.getMonthlyIncome()).isEqualByComparingTo("4500.00");
        assertThat(simulated.getCreditHistory().revolvingUtilization()).isEqualTo(0.10);
        assertThat(simulated.getRequestedAmount()).isEqualTo(application.getRequestedAmount());
        assertThat(simulated.getCreditHistory().latePayments30To59Days())
                .isEqualTo(application.getCreditHistory().latePayments30To59Days());
        assertThat(application.getMonthlyIncome()).isEqualByComparingTo("3000.00");
        assertThat(application.getCreditHistory().revolvingUtilization()).isEqualTo(0.35);
    }

    @Test
    void describe_listaSoloLosCambiosConValorRealYSimulado() {
        var described = changes(new BigDecimal("4500.00"), 0.10).describe(application);

        assertThat(described).containsExactly(
                new VariableChange("monthlyIncome", new BigDecimal("3000.00"), new BigDecimal("4500.00")),
                new VariableChange("creditHistory.revolvingUtilization", new BigDecimal("0.35"), new BigDecimal("0.1")));
    }

    @Test
    void sinNingunCambio_noEsUnaSimulacionValida() {
        assertThatThrownBy(() -> changes(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conValoresImposibles_seRechaza() {
        assertThatThrownBy(() -> changes(BigDecimal.ZERO, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> changes(null, -0.1)).isInstanceOf(IllegalArgumentException.class);
    }
}
