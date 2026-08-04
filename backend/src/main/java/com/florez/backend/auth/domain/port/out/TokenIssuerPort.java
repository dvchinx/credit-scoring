package com.florez.backend.auth.domain.port.out;

import com.florez.backend.auth.domain.model.Role;

public interface TokenIssuerPort {

    IssuedToken issue(String username, Role role);

    /**
     * @throws com.florez.backend.common.domain.InvalidTokenException si el token es inválido, expiró o fue alterado.
     */
    TokenClaims validate(String token);

    record IssuedToken(String token, String tokenType, long expiresInMs) {
    }

    record TokenClaims(String username, Role role) {
    }
}
