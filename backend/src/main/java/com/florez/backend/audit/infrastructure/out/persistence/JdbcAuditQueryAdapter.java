package com.florez.backend.audit.infrastructure.out.persistence;

import com.florez.backend.audit.domain.model.AuditSummary;
import com.florez.backend.audit.domain.model.AuditSummary.FailedAttempt;
import com.florez.backend.audit.domain.model.AuditSummary.ModelVersionUsage;
import com.florez.backend.audit.domain.model.AuditSummary.SimulationStats;
import com.florez.backend.audit.domain.port.out.AuditQueryPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agregados calculados en SQL sobre las tablas append-only. Incluye las decisiones de solicitudes
 * eliminadas (soft-delete): para auditoría cuenta todo lo que se decidió.
 */
@Component
public class JdbcAuditQueryAdapter implements AuditQueryPort {

    private static final List<String> OUTCOMES = List.of("APPROVED", "REJECTED", "MANUAL_REVIEW");

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditSummary summarize(int recentFailuresLimit) {
        Map<String, Long> byOutcome = new LinkedHashMap<>();
        OUTCOMES.forEach(outcome -> byOutcome.put(outcome, 0L));
        jdbcTemplate.query("""
                SELECT outcome, count(*) AS total FROM credit_decisions
                WHERE status = 'COMPLETED' GROUP BY outcome
                """, rs -> {
            byOutcome.put(rs.getString("outcome"), rs.getLong("total"));
        });
        long completed = byOutcome.values().stream().mapToLong(Long::longValue).sum();
        Long failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM credit_decisions WHERE status = 'FAILED'", Long.class);

        return new AuditSummary(
                completed,
                failed == null ? 0 : failed,
                byOutcome,
                usageByModelVersion(),
                recentFailures(recentFailuresLimit),
                simulationStats());
    }

    private List<ModelVersionUsage> usageByModelVersion() {
        Map<String, long[]> counts = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT model_version, outcome, count(*) AS total FROM credit_decisions
                WHERE status = 'COMPLETED'
                GROUP BY model_version, outcome
                ORDER BY model_version DESC
                """, rs -> {
            long[] row = counts.computeIfAbsent(rs.getString("model_version"), version -> new long[OUTCOMES.size()]);
            row[OUTCOMES.indexOf(rs.getString("outcome"))] = rs.getLong("total");
        });
        List<ModelVersionUsage> usage = new ArrayList<>();
        counts.forEach((version, row) -> usage.add(new ModelVersionUsage(version, row[0], row[1], row[2])));
        return usage;
    }

    private List<FailedAttempt> recentFailures(int limit) {
        return jdbcTemplate.query("""
                SELECT id, credit_application_id, failure_reason, decided_by, decided_at FROM credit_decisions
                WHERE status = 'FAILED'
                ORDER BY decided_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new FailedAttempt(
                rs.getObject("id", UUID.class),
                rs.getObject("credit_application_id", UUID.class),
                rs.getString("failure_reason"),
                rs.getString("decided_by"),
                rs.getTimestamp("decided_at").toInstant()), limit);
    }

    private SimulationStats simulationStats() {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE baseline_outcome <> scenario_outcome) AS outcome_changed
                FROM what_if_simulations
                """, (rs, rowNum) -> new SimulationStats(rs.getLong("total"), rs.getLong("outcome_changed")));
    }
}
