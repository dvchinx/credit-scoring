package com.florez.backend.audit.domain.port.out;

import com.florez.backend.audit.domain.model.AuditSummary;

/** Consultas de solo lectura sobre los registros append-only de decisiones y simulaciones. */
public interface AuditQueryPort {

    AuditSummary summarize(int recentFailuresLimit);
}
