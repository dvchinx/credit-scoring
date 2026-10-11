package com.florez.backend.modelregistry.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** Fila append-only de {@code model_activations}; el id secuencial ordena las activaciones. */
@Entity
@Immutable
@Table(name = "model_activations")
public class ModelActivationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(nullable = false)
    private String reason;

    @Column(name = "activated_by", nullable = false)
    private String activatedBy;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    protected ModelActivationJpaEntity() {
        // requerido por JPA
    }

    public ModelActivationJpaEntity(String modelVersion, String reason, String activatedBy, Instant activatedAt) {
        this.modelVersion = modelVersion;
        this.reason = reason;
        this.activatedBy = activatedBy;
        this.activatedAt = activatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public String getReason() {
        return reason;
    }

    public String getActivatedBy() {
        return activatedBy;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }
}
