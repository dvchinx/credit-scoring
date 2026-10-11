package com.florez.backend.whatif.infrastructure.out.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskDirection;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort.Comparison;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MlServiceWhatIfAdapterTest {

    private static final ModelFeatures BASELINE = new ModelFeatures(0.35, 36, 1, 0.27, 3000, 6, 0, 1, 0, 1);
    private static final ModelFeatures SCENARIO = new ModelFeatures(0.05, 36, 1, 0.27, 3000, 6, 0, 1, 0, 1);

    private MockRestServiceServer server;
    private MlServiceWhatIfAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml-service");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new MlServiceWhatIfAdapter(builder.build());
    }

    private static String evaluation(double pd, int score, double shap) {
        return """
                {"probability_of_default": %s, "risk_score": %d, "base_value": -0.4, "output_value": -0.1,
                 "contributions": [{"feature": "revolving_utilization_of_unsecured_lines", "value": 0.35,
                                    "shap_value": %s, "direction": "%s"}]}
                """.formatted(pd, score, shap, shap > 0 ? "increases_risk" : "decreases_risk");
    }

    @Test
    void compare_enviaAmbosEscenariosYMapeaLaRespuesta() {
        server.expect(requestTo("http://ml-service/what-if?model_version=v1"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.baseline.revolving_utilization_of_unsecured_lines").value(0.35))
                .andExpect(jsonPath("$.scenario.revolving_utilization_of_unsecured_lines").value(0.05))
                .andExpect(jsonPath("$.scenario.age").value(36))
                .andRespond(withSuccess("""
                        {"model_version": "v1", "baseline": %s, "scenario": %s}
                        """.formatted(evaluation(0.45, 602, 0.3), evaluation(0.2, 740, -0.2)), MediaType.APPLICATION_JSON));

        Comparison comparison = adapter.compare("v1", BASELINE, SCENARIO);

        assertThat(comparison.baseline().risk().probabilityOfDefault()).isEqualTo(0.45);
        assertThat(comparison.scenario().risk().riskScore()).isEqualTo(740);
        assertThat(comparison.scenario().risk().modelVersion()).isEqualTo("v1");
        assertThat(comparison.scenario().explanation().contributions().getFirst().direction())
                .isEqualTo(RiskDirection.DECREASES_RISK);
        server.verify();
    }

    @Test
    void compare_conRespuestaIncompleta_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/what-if?model_version=v1"))
                .andRespond(withSuccess("{\"model_version\": \"v1\", \"baseline\": %s}".formatted(evaluation(0.4, 630, 0.1)),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.compare("v1", BASELINE, SCENARIO))
                .isInstanceOf(ScoringUnavailableException.class)
                .hasMessageContaining("/what-if");
    }

    @Test
    void compare_conVersionInexistenteEnElMl_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/what-if?model_version=v1")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.compare("v1", BASELINE, SCENARIO)).isInstanceOf(ScoringUnavailableException.class);
    }
}
