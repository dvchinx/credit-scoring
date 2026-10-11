package com.florez.backend.audit.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vista agregada de las decisiones automatizadas: cuántas hubo por resultado y por versión de
 * modelo, qué intentos fallaron y cuántas simulaciones cambiaron el resultado.
 *
 * @param decisionsByOutcome APPROVED, REJECTED y MANUAL_REVIEW, siempre presentes (0 si no hay)
 */
public record AuditSummary(
        long completedDecisions,
        long failedDecisions,
        Map<String, Long> decisionsByOutcome,
        List<ModelVersionUsage> decisionsByModelVersion,
        List<FailedAttempt> recentFailures,
        SimulationStats simulations) {

    public record ModelVersionUsage(String modelVersion, long approved, long rejected, long manualReview) {

        public long total() {
            return approved + rejected + manualReview;
        }
    }

    public record FailedAttempt(
            UUID decisionId, UUID creditApplicationId, String failureReason, String decidedBy, Instant decidedAt) {
    }

    /** @param outcomeChanged simulaciones cuyo resultado simulado difiere del real */
    public record SimulationStats(long total, long outcomeChanged) {
    }
}
