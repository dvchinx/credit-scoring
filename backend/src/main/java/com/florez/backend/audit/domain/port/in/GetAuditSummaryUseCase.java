package com.florez.backend.audit.domain.port.in;

import com.florez.backend.audit.domain.model.AuditSummary;

public interface GetAuditSummaryUseCase {

    AuditSummary summary();
}
