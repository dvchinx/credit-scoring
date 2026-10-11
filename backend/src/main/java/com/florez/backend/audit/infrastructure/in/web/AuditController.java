package com.florez.backend.audit.infrastructure.in.web;

import com.florez.backend.audit.domain.model.AuditSummary;
import com.florez.backend.audit.domain.port.in.GetAuditSummaryUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final GetAuditSummaryUseCase summaryUseCase;

    public AuditController(GetAuditSummaryUseCase summaryUseCase) {
        this.summaryUseCase = summaryUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<AuditSummaryResponse> summary() {
        return ResponseEntity.ok(AuditSummaryResponse.from(summaryUseCase.summary()));
    }

    public record AuditSummaryResponse(
            long completedDecisions,
            long failedDecisions,
            Map<String, Long> decisionsByOutcome,
            List<ModelVersionUsageResponse> decisionsByModelVersion,
            List<FailedAttemptResponse> recentFailures,
            SimulationStatsResponse simulations) {

        static AuditSummaryResponse from(AuditSummary summary) {
            return new AuditSummaryResponse(
                    summary.completedDecisions(),
                    summary.failedDecisions(),
                    summary.decisionsByOutcome(),
                    summary.decisionsByModelVersion().stream()
                            .map(usage -> new ModelVersionUsageResponse(usage.modelVersion(), usage.approved(),
                                    usage.rejected(), usage.manualReview(), usage.total()))
                            .toList(),
                    summary.recentFailures().stream()
                            .map(failure -> new FailedAttemptResponse(failure.decisionId(), failure.creditApplicationId(),
                                    failure.failureReason(), failure.decidedBy(), failure.decidedAt()))
                            .toList(),
                    new SimulationStatsResponse(summary.simulations().total(), summary.simulations().outcomeChanged()));
        }
    }

    public record ModelVersionUsageResponse(String modelVersion, long approved, long rejected, long manualReview, long total) {
    }

    public record FailedAttemptResponse(
            UUID decisionId, UUID creditApplicationId, String failureReason, String decidedBy, Instant decidedAt) {
    }

    public record SimulationStatsResponse(long total, long outcomeChanged) {
    }
}
