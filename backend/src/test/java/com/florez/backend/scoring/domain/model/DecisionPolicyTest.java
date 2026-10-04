package com.florez.backend.scoring.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.scoring.ScoringTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DecisionPolicyTest {

    private final DecisionPolicy policy = ScoringTestData.POLICY;

    private static ModelFeatures features(int age, double debtRatio) {
        return new ModelFeatures(0.3, age, 0, debtRatio, 4000, 5, 0, 1, 0, 1);
    }

    @ParameterizedTest
    @CsvSource({
            "0.05, APPROVED",
            "0.30, APPROVED",
            "0.31, MANUAL_REVIEW",
            "0.59, MANUAL_REVIEW",
            "0.60, REJECTED",
            "0.95, REJECTED"
    })
    void evaluate_aplicaLosUmbralesDeProbabilidadDeDefault(double probability, DecisionOutcome expected) {
        PolicyResult result = policy.evaluate(features(40, 0.2), ScoringTestData.risk(probability, "v1"));

        assertThat(result.outcome()).isEqualTo(expected);
        assertThat(result.reasons()).hasSize(1);
    }

    @Test
    void evaluate_conEndeudamientoExcesivo_rechazaAunqueElRiesgoSeaBajo() {
        PolicyResult result = policy.evaluate(features(40, 0.65), ScoringTestData.risk(0.05, "v1"));

        assertThat(result.outcome()).isEqualTo(DecisionOutcome.REJECTED);
        assertThat(result.reasons()).singleElement().asString().contains("65.0%").contains("50.0%");
    }

    @Test
    void evaluate_conMenorDeEdad_rechazaYAcumulaTodasLasReglasDurasIncumplidas() {
        PolicyResult result = policy.evaluate(features(17, 0.65), ScoringTestData.risk(0.05, "v1"));

        assertThat(result.outcome()).isEqualTo(DecisionOutcome.REJECTED);
        assertThat(result.reasons()).hasSize(2);
        assertThat(result.reasons().getFirst()).contains("17 años");
    }

    @Test
    void constructor_conUmbralDeAprobacionNoMenorAlDeRechazo_fallaAlArrancar() {
        assertThatThrownBy(() -> new DecisionPolicy(0.6, 0.6, 0.5, 18)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DecisionPolicy(0.3, 1.2, 0.5, 18)).isInstanceOf(IllegalArgumentException.class);
    }
}
