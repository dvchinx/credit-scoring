package com.florez.backend.auth.infrastructure.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "el username es obligatorio") String username,
        @NotBlank(message = "la contraseña es obligatoria") String password) {
}
