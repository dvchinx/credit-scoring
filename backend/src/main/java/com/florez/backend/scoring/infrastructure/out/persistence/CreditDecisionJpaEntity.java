package com.florez.backend.scoring.infrastructure.out.persistence;

import com.florez.backend.scoring.domain.model.DecisionOutcome;
import com.florez.backend.scoring.domain.model.DecisionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * Fila append-only de {@code credit_decisions}. {@link Immutable} evita que Hibernate emita
 * UPDATEs; además, un trigger en la base de datos rechaza cualquier UPDATE o DELETE.
 */
@Entity
@Immutable
@Table(name = "credit_decisions")
public class CreditDecisionJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "credit_application_id", nullable = false)
    private UUID creditApplicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionStatus status;

    @Enumerated(EnumType.STRING)
    private DecisionOutcome outcome;

    @Column(name = "probability_of_default")
    private Double probabilityOfDefault;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "model_version")
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "application_snapshot", nullable = false)
    private String applicationSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_input", nullable = false)
    private String modelInput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "explanation")
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "policy", nullable = false)
    private String policy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasons", nullable = false)
    private String reasons;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "decided_by", nullable = false)
    private String decidedBy;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected CreditDecisionJpaEntity() {
        // requerido por JPA
    }

    public CreditDecisionJpaEntity(
            UUID creditApplicationId,
            DecisionStatus status,
            DecisionOutcome outcome,
            Double probabilityOfDefault,
            Integer riskScore,
            String modelVersion,
            String applicationSnapshot,
            String modelInput,
            String explanation,
            String policy,
            String reasons,
            String failureReason,
            String decidedBy,
            Instant decidedAt) {
        this.creditApplicationId = creditApplicationId;
        this.status = status;
        this.outcome = outcome;
        this.probabilityOfDefault = probabilityOfDefault;
        this.riskScore = riskScore;
        this.modelVersion = modelVersion;
        this.applicationSnapshot = applicationSnapshot;
        this.modelInput = modelInput;
        this.explanation = explanation;
        this.policy = policy;
        this.reasons = reasons;
        this.failureReason = failureReason;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreditApplicationId() {
        return creditApplicationId;
    }

    public DecisionStatus getStatus() {
        return status;
    }

    public DecisionOutcome getOutcome() {
        return outcome;
    }

    public Double getProbabilityOfDefault() {
        return probabilityOfDefault;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getApplicationSnapshot() {
        return applicationSnapshot;
    }

    public String getModelInput() {
        return modelInput;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getPolicy() {
        return policy;
    }

    public String getReasons() {
        return reasons;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
