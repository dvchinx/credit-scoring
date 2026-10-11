package com.florez.backend.modelregistry.infrastructure.in.web.dto;

import com.florez.backend.modelregistry.domain.model.ModelActivation;
import java.time.Instant;

public record ModelActivationResponse(Long id, String modelVersion, String reason, String activatedBy, Instant activatedAt) {

    public static ModelActivationResponse from(ModelActivation activation) {
        return new ModelActivationResponse(
                activation.id(), activation.modelVersion(), activation.reason(), activation.activatedBy(),
                activation.activatedAt());
    }
}
