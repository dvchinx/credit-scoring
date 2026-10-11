package com.florez.backend.scoring.infrastructure.out.ml;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlExplainResponse;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlScoreResponse;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP del microservicio de ML (FastAPI): endpoints {@code /score} y {@code /explain}. */
@Component
public class MlServiceScoringAdapter implements ScoringModelPort {

    private static final Logger log = LoggerFactory.getLogger(MlServiceScoringAdapter.class);

    private final RestClient restClient;

    public MlServiceScoringAdapter(RestClient mlServiceRestClient) {
        this.restClient = mlServiceRestClient;
    }

    @Override
    public RiskAssessment score(String modelVersion, ModelFeatures features) {
        MlScoreResponse response = post(restClient, "/score", modelVersion, features.asFeatureMap(), MlScoreResponse.class);
        if (anyNull(response.probabilityOfDefault(), response.riskScore(), response.modelVersion())) {
            throw MlContributionMapper.invalid("/score");
        }
        return new RiskAssessment(response.probabilityOfDefault(), response.riskScore(), response.modelVersion());
    }

    @Override
    public Explanation explain(String modelVersion, ModelFeatures features) {
        MlExplainResponse response =
                post(restClient, "/explain", modelVersion, features.asFeatureMap(), MlExplainResponse.class);
        if (anyNull(response.modelVersion(), response.baseValue(), response.outputValue())) {
            throw MlContributionMapper.invalid("/explain");
        }
        return new Explanation(response.modelVersion(), response.baseValue(), response.outputValue(),
                MlContributionMapper.toDomain(response.contributions(), "/explain"));
    }

    /**
     * POST al servicio de ML fijando la versión de modelo.
     *
     * @throws ScoringUnavailableException si el servicio no responde, devuelve error o un cuerpo vacío
     */
    public static <T> T post(RestClient restClient, String path, String modelVersion, Object body, Class<T> responseType) {
        T response;
        try {
            response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path(path).queryParam("model_version", modelVersion).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            log.warn("Fallo llamando al servicio de ML en {}: {}", path, e.getMessage());
            throw new ScoringUnavailableException("el servicio de ML no respondió correctamente en " + path, e);
        }
        if (response == null) {
            throw MlContributionMapper.invalid(path);
        }
        return response;
    }

    public static boolean anyNull(Object... values) {
        return Stream.of(values).anyMatch(Objects::isNull);
    }
}
