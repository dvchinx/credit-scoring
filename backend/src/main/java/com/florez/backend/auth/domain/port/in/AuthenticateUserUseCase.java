package com.florez.backend.auth.domain.port.in;

import com.florez.backend.auth.domain.model.Role;

public interface AuthenticateUserUseCase {

    AuthenticatedSession authenticate(String username, String rawPassword);

    record AuthenticatedSession(String token, String tokenType, long expiresInMs, String username, Role role) {
    }
}
