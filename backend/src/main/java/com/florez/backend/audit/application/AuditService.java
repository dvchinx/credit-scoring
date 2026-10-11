package com.florez.backend.audit.application;

import com.florez.backend.audit.domain.model.AuditSummary;
import com.florez.backend.audit.domain.port.in.GetAuditSummaryUseCase;
import com.florez.backend.audit.domain.port.out.AuditQueryPort;

public final class AuditService implements GetAuditSummaryUseCase {

    private static final int RECENT_FAILURES_LIMIT = 10;

    private final AuditQueryPort auditQuery;

    public AuditService(AuditQueryPort auditQuery) {
        this.auditQuery = auditQuery;
    }

    @Override
    public AuditSummary summary() {
        return auditQuery.summarize(RECENT_FAILURES_LIMIT);
    }
}
