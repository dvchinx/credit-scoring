package com.florez.backend.whatif.infrastructure.out.persistence;

import com.florez.backend.scoring.domain.model.DecisionOutcome;
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

/** Fila append-only de {@code what_if_simulations}. */
@Entity
@Immutable
@Table(name = "what_if_simulations")
public class WhatIfSimulationJpaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "credit_application_id", nullable = false)
    private UUID creditApplicationId;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String changes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String baseline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "baseline_outcome", nullable = false)
    private DecisionOutcome baselineOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_outcome", nullable = false)
    private DecisionOutcome scenarioOutcome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String policy;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "simulated_at", nullable = false)
    private Instant simulatedAt;

    protected WhatIfSimulationJpaEntity() {
        // requerido por JPA
    }

    public WhatIfSimulationJpaEntity(
            UUID creditApplicationId,
            String modelVersion,
            String changes,
            String baseline,
            String scenario,
            DecisionOutcome baselineOutcome,
            DecisionOutcome scenarioOutcome,
            String policy,
            String requestedBy,
            Instant simulatedAt) {
        this.creditApplicationId = creditApplicationId;
        this.modelVersion = modelVersion;
        this.changes = changes;
        this.baseline = baseline;
        this.scenario = scenario;
        this.baselineOutcome = baselineOutcome;
        this.scenarioOutcome = scenarioOutcome;
        this.policy = policy;
        this.requestedBy = requestedBy;
        this.simulatedAt = simulatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreditApplicationId() {
        return creditApplicationId;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getChanges() {
        return changes;
    }

    public String getBaseline() {
        return baseline;
    }

    public String getScenario() {
        return scenario;
    }

    public String getPolicy() {
        return policy;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getSimulatedAt() {
        return simulatedAt;
    }
}
