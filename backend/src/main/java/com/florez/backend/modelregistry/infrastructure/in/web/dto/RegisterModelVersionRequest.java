package com.florez.backend.modelregistry.infrastructure.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterModelVersionRequest(
        @NotBlank(message = "la versión es obligatoria")
        @Pattern(regexp = "^[0-9A-Za-z_-]{1,100}$", message = "formato de versión inválido") String version) {
}
