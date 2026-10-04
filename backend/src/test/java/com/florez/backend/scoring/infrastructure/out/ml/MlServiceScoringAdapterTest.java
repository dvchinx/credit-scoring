package com.florez.backend.scoring.infrastructure.out.ml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.model.RiskDirection;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MlServiceScoringAdapterTest {

    private static final ModelFeatures FEATURES = new ModelFeatures(0.3, 45, 0, 0.2, 5000, 5, 0, 1, 0, 1);

    private MockRestServiceServer server;
    private MlServiceScoringAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ml-service");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new MlServiceScoringAdapter(builder.build());
    }

    @Test
    void score_enviaLasFeaturesConNombresCanonicosYMapeaLaRespuesta() {
        server.expect(requestTo("http://ml-service/score"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.revolving_utilization_of_unsecured_lines").value(0.3))
                .andExpect(jsonPath("$.age").value(45))
                .andExpect(jsonPath("$.debt_ratio").value(0.2))
                .andExpect(jsonPath("$.number_of_dependents").value(1))
                .andRespond(withSuccess("""
                        {"probability_of_default": 0.123, "risk_score": 782, "model_version": "20261004T120000Z"}
                        """, MediaType.APPLICATION_JSON));

        RiskAssessment risk = adapter.score(FEATURES);

        assertThat(risk).isEqualTo(new RiskAssessment(0.123, 782, "20261004T120000Z"));
        server.verify();
    }

    @Test
    void explain_mapeaContribucionesYDireccion() {
        server.expect(requestTo("http://ml-service/explain"))
                .andRespond(withSuccess("""
                        {"model_version": "v1", "base_value": -0.4, "output_value": -0.9,
                         "contributions": [
                           {"feature": "age", "value": 45, "shap_value": -0.6, "direction": "decreases_risk"},
                           {"feature": "debt_ratio", "value": 0.2, "shap_value": 0.1, "direction": "increases_risk"}
                         ]}
                        """, MediaType.APPLICATION_JSON));

        Explanation explanation = adapter.explain(FEATURES);

        assertThat(explanation.modelVersion()).isEqualTo("v1");
        assertThat(explanation.baseValue()).isEqualTo(-0.4);
        assertThat(explanation.contributions()).hasSize(2);
        assertThat(explanation.contributions().getFirst().feature()).isEqualTo("age");
        assertThat(explanation.contributions().getFirst().direction()).isEqualTo(RiskDirection.DECREASES_RISK);
    }

    @Test
    void score_conModeloNoCargado_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/score")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.score(FEATURES))
                .isInstanceOf(ScoringUnavailableException.class)
                .hasMessageContaining("/score");
    }

    @Test
    void score_conErrorDeConexion_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/score")).andRespond(withException(new IOException("connection refused")));

        assertThatThrownBy(() -> adapter.score(FEATURES)).isInstanceOf(ScoringUnavailableException.class);
    }

    @Test
    void score_conRespuestaIncompleta_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/score"))
                .andRespond(withSuccess("{\"probability_of_default\": 0.1}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.score(FEATURES))
                .isInstanceOf(ScoringUnavailableException.class)
                .hasMessageContaining("respuesta inválida");
    }

    @Test
    void explain_sinContribuciones_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/explain"))
                .andRespond(withSuccess("""
                        {"model_version": "v1", "base_value": -0.4, "output_value": -0.4, "contributions": []}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.explain(FEATURES)).isInstanceOf(ScoringUnavailableException.class);
    }

    @Test
    void explain_conDireccionDesconocida_lanzaScoringUnavailable() {
        server.expect(requestTo("http://ml-service/explain"))
                .andRespond(withSuccess("""
                        {"model_version": "v1", "base_value": -0.4, "output_value": -0.9,
                         "contributions": [{"feature": "age", "value": 45, "shap_value": -0.6, "direction": "sideways"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.explain(FEATURES)).isInstanceOf(ScoringUnavailableException.class);
    }
}
