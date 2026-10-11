package com.florez.backend.scoring.infrastructure.out.ml;

import com.florez.backend.scoring.domain.exception.ScoringUnavailableException;
import com.florez.backend.scoring.domain.model.FeatureContribution;
import com.florez.backend.scoring.domain.model.RiskDirection;
import com.florez.backend.scoring.infrastructure.out.ml.dto.MlFeatureContribution;
import java.util.List;

/** Traduce las contribuciones SHAP que devuelve el servicio de ML, rechazando respuestas incompletas. */
public final class MlContributionMapper {

    private MlContributionMapper() {
    }

    /** @throws ScoringUnavailableException si falta algún dato o la lista viene vacía */
    public static List<FeatureContribution> toDomain(List<MlFeatureContribution> contributions, String path) {
        if (contributions == null || contributions.isEmpty()) {
            throw invalid(path);
        }
        return contributions.stream().map(contribution -> toDomain(contribution, path)).toList();
    }

    private static FeatureContribution toDomain(MlFeatureContribution contribution, String path) {
        if (contribution.feature() == null || contribution.value() == null || contribution.shapValue() == null
                || contribution.direction() == null) {
            throw invalid(path);
        }
        RiskDirection direction = switch (contribution.direction()) {
            case "increases_risk" -> RiskDirection.INCREASES_RISK;
            case "decreases_risk" -> RiskDirection.DECREASES_RISK;
            case "neutral" -> RiskDirection.NEUTRAL;
            default -> throw invalid(path);
        };
        return new FeatureContribution(contribution.feature(), contribution.value(), contribution.shapValue(), direction);
    }

    public static ScoringUnavailableException invalid(String path) {
        return new ScoringUnavailableException("el servicio de ML devolvió una respuesta inválida en " + path);
    }
}
