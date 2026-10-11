package com.florez.backend.modelregistry.infrastructure.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateModelVersionRequest(
        @NotBlank(message = "la versión es obligatoria") String version,
        @NotBlank(message = "el motivo de la activación es obligatorio")
        @Size(max = 1000, message = "el motivo no puede superar los 1000 caracteres") String reason) {
}
