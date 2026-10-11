package com.florez.backend.auth.infrastructure.in.web.dto;

import com.florez.backend.auth.domain.model.Role;

/** {@code username} y {@code role} evitan que el cliente tenga que decodificar el JWT para adaptar su interfaz. */
public record LoginResponse(String token, String tokenType, long expiresInMs, String username, Role role) {
}
