package com.florez.backend.modelregistry.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Versión de modelo registrada en el backend. Inmutable: una vez registrada, su metadata no cambia. */
public record ModelVersion(ModelArtifact artifact, String registeredBy, Instant registeredAt) {

    public ModelVersion {
        Objects.requireNonNull(artifact, "artifact");
        Objects.requireNonNull(registeredBy, "registeredBy");
        Objects.requireNonNull(registeredAt, "registeredAt");
    }

    public String version() {
        return artifact.version();
    }
}
