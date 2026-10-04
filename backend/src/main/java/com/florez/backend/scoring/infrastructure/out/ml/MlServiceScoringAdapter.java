package com.florez.backend.scoring.infrastructure.out.ml;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.FeatureContribution;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.domain.model.RiskDirection;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlExplainResponse;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlFeatureContribution;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlScoreResponse;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP del microservicio de ML (FastAPI): endpoints {@code /score} y {@code /explain}. */
public class MlServiceScoringAdapter implements ScoringModelPort {

    private static final Logger log = LoggerFactory.getLogger(MlServiceScoringAdapter.class);

    private final RestClient restClient;

    public MlServiceScoringAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RiskAssessment score(ModelFeatures features) {
        MlScoreResponse response = post("/score", features, MlScoreResponse.class);
        if (anyNull(response.probabilityOfDefault(), response.riskScore(), response.modelVersion())) {
            throw invalidResponse("/score");
        }
        return new RiskAssessment(response.probabilityOfDefault(), response.riskScore(), response.modelVersion());
    }

    @Override
    public Explanation explain(ModelFeatures features) {
        MlExplainResponse response = post("/explain", features, MlExplainResponse.class);
        if (anyNull(response.modelVersion(), response.baseValue(), response.outputValue(), response.contributions())
                || response.contributions().isEmpty()) {
            throw invalidResponse("/explain");
        }
        List<FeatureContribution> contributions = response.contributions().stream()
                .map(this::toDomain)
                .toList();
        return new Explanation(response.modelVersion(), response.baseValue(), response.outputValue(), contributions);
    }

    private FeatureContribution toDomain(MlFeatureContribution contribution) {
        if (anyNull(contribution.feature(), contribution.value(), contribution.shapValue(), contribution.direction())) {
            throw invalidResponse("/explain");
        }
        return new FeatureContribution(
                contribution.feature(), contribution.value(), contribution.shapValue(), toDirection(contribution.direction()));
    }

    private RiskDirection toDirection(String direction) {
        return switch (direction) {
            case "increases_risk" -> RiskDirection.INCREASES_RISK;
            case "decreases_risk" -> RiskDirection.DECREASES_RISK;
            case "neutral" -> RiskDirection.NEUTRAL;
            default -> throw invalidResponse("/explain");
        };
    }

    private <T> T post(String path, ModelFeatures features, Class<T> responseType) {
        T body;
        try {
            body = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(features.asFeatureMap())
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            log.warn("Fallo llamando al servicio de ML en {}: {}", path, e.getMessage());
            throw new ScoringUnavailableException("el servicio de ML no respondió correctamente en " + path, e);
        }
        if (body == null) {
            throw invalidResponse(path);
        }
        return body;
    }

    private static ScoringUnavailableException invalidResponse(String path) {
        log.warn("Respuesta incompleta o inválida del servicio de ML en {}", path);
        return new ScoringUnavailableException("el servicio de ML devolvió una respuesta inválida en " + path);
    }

    private static boolean anyNull(Object... values) {
        return Stream.of(values).anyMatch(Objects::isNull);
    }
}
