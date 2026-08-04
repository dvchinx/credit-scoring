package com.florez.backend.auth.infrastructure.out.security;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.common.domain.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuerAdapter implements TokenIssuerPort {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE = "Bearer";

    private final Key signingKey;
    private final long expirationMs;

    public JwtTokenIssuerAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public IssuedToken issue(String username, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();

        return new IssuedToken(token, TOKEN_TYPE, expirationMs);
    }

    @Override
    public TokenClaims validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
            return new TokenClaims(username, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token JWT inválido o expirado");
        }
    }
}
