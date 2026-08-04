package com.florez.backend.auth.infrastructure.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.common.domain.InvalidTokenException;
import org.junit.jupiter.api.Test;

class JwtTokenIssuerAdapterTest {

    private static final String SECRET = "test-secret-of-at-least-32-characters-long";

    @Test
    void issueYValidate_conTokenRecienEmitido_devuelveLosClaimsOriginales() {
        JwtTokenIssuerAdapter adapter = new JwtTokenIssuerAdapter(SECRET, 60_000L);

        TokenIssuerPort.IssuedToken issued = adapter.issue("analyst1", Role.ANALYST);
        TokenIssuerPort.TokenClaims claims = adapter.validate(issued.token());

        assertThat(claims.username()).isEqualTo("analyst1");
        assertThat(claims.role()).isEqualTo(Role.ANALYST);
        assertThat(issued.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void validate_conTokenExpirado_lanzaInvalidToken() {
        JwtTokenIssuerAdapter adapter = new JwtTokenIssuerAdapter(SECRET, -1000L);

        TokenIssuerPort.IssuedToken issued = adapter.issue("analyst1", Role.ANALYST);

        assertThatThrownBy(() -> adapter.validate(issued.token())).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validate_conTokenAlterado_lanzaInvalidToken() {
        JwtTokenIssuerAdapter adapter = new JwtTokenIssuerAdapter(SECRET, 60_000L);
        TokenIssuerPort.IssuedToken issued = adapter.issue("analyst1", Role.ANALYST);
        String tampered = issued.token() + "tampered";

        assertThatThrownBy(() -> adapter.validate(tampered)).isInstanceOf(InvalidTokenException.class);
    }
}
