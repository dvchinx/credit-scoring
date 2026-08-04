package com.florez.backend.auth.domain.port.in;

public interface AuthenticateUserUseCase {

    AuthenticatedSession authenticate(String username, String rawPassword);

    record AuthenticatedSession(String token, String tokenType, long expiresInMs) {
    }
}
