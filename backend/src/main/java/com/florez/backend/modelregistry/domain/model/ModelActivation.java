package com.florez.backend.modelregistry.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Evento de activación: desde {@code activatedAt}, las evaluaciones usan {@code modelVersion}
 * hasta la siguiente activación. El historial completo responde "qué modelo decidía en tal fecha";
 * un rollback es simplemente activar de nuevo una versión anterior.
 */
public record ModelActivation(Long id, String modelVersion, String reason, String activatedBy, Instant activatedAt) {

    public ModelActivation {
        Objects.requireNonNull(modelVersion, "modelVersion");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Toda activación debe justificarse con un motivo");
        }
        Objects.requireNonNull(activatedBy, "activatedBy");
        Objects.requireNonNull(activatedAt, "activatedAt");
    }

    public static ModelActivation create(String modelVersion, String reason, String activatedBy, Instant activatedAt) {
        return new ModelActivation(null, modelVersion, reason.strip(), activatedBy, activatedAt);
    }
}
