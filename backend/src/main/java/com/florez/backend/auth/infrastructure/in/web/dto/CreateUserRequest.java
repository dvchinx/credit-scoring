package com.florez.backend.auth.infrastructure.in.web.dto;

import com.florez.backend.auth.domain.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "el username es obligatorio") String username,
        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, message = "la contraseña debe tener al menos 8 caracteres") String password,
        @NotNull(message = "el rol es obligatorio") Role role) {
}
