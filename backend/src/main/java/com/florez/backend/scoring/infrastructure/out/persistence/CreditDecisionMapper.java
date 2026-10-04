package com.florez.backend.scoring.infrastructure.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.florez.backend.scoring.domain.model.ApplicationSnapshot;
import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import java.util.List;
import java.util.Map;

/**
 * Traduce entre {@link CreditDecision} y su fila. Los snapshots se guardan como JSONB; la
 * entrada del modelo usa los nombres canónicos de feature, de modo que el JSON almacenado es
 * exactamente el payload que se envió a {@code /score} y {@code /explain}.
 */
final class CreditDecisionMapper {

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private CreditDecisionMapper() {
    }

    static CreditDecisionJpaEntity toNewEntity(CreditDecision decision) {
        RiskAssessment risk = decision.getRiskAssessment();
        return new CreditDecisionJpaEntity(
                decision.getCreditApplicationId(),
                decision.getStatus(),
                decision.getOutcome(),
                risk == null ? null : risk.probabilityOfDefault(),
                risk == null ? null : risk.riskScore(),
                risk == null ? null : risk.modelVersion(),
                write(decision.getApplicationSnapshot()),
                write(decision.getModelInput().asFeatureMap()),
                decision.getExplanation() == null ? null : write(decision.getExplanation()),
                write(decision.getPolicy()),
                write(decision.getReasons()),
                decision.getFailureReason(),
                decision.getDecidedBy(),
                decision.getDecidedAt());
    }

    static CreditDecision toDomain(CreditDecisionJpaEntity entity) {
        RiskAssessment risk = entity.getModelVersion() == null || entity.getProbabilityOfDefault() == null
                ? null
                : new RiskAssessment(entity.getProbabilityOfDefault(), entity.getRiskScore(), entity.getModelVersion());
        return CreditDecision.reconstitute(
                entity.getId(),
                entity.getCreditApplicationId(),
                entity.getStatus(),
                entity.getOutcome(),
                read(entity.getApplicationSnapshot(), new TypeReference<ApplicationSnapshot>() { }),
                toModelFeatures(read(entity.getModelInput(), new TypeReference<Map<String, Number>>() { })),
                risk,
                entity.getExplanation() == null ? null : read(entity.getExplanation(), new TypeReference<Explanation>() { }),
                read(entity.getPolicy(), new TypeReference<DecisionPolicy>() { }),
                read(entity.getReasons(), new TypeReference<List<String>>() { }),
                entity.getFailureReason(),
                entity.getDecidedBy(),
                entity.getDecidedAt());
    }

    private static ModelFeatures toModelFeatures(Map<String, Number> features) {
        return new ModelFeatures(
                feature(features, "revolving_utilization_of_unsecured_lines").doubleValue(),
                feature(features, "age").intValue(),
                feature(features, "number_of_time_30_59_days_past_due_not_worse").intValue(),
                feature(features, "debt_ratio").doubleValue(),
                feature(features, "monthly_income").doubleValue(),
                feature(features, "number_of_open_credit_lines_and_loans").intValue(),
                feature(features, "number_of_times_90_days_late").intValue(),
                feature(features, "number_real_estate_loans_or_lines").intValue(),
                feature(features, "number_of_time_60_89_days_past_due_not_worse").intValue(),
                feature(features, "number_of_dependents").intValue());
    }

    private static Number feature(Map<String, Number> features, String name) {
        Number value = features.get(name);
        if (value == null) {
            throw new IllegalStateException("La entrada del modelo almacenada no contiene la feature '" + name + "'");
        }
        return value;
    }

    private static String write(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la decisión a JSON", e);
        }
    }

    private static <T> T read(String json, TypeReference<T> type) {
        try {
            return JSON.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer la decisión almacenada", e);
        }
    }
}
