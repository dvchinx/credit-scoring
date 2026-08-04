package com.florez.backend.auth.infrastructure.out.security;

import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.common.domain.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenIssuerPort tokenIssuerPort;

    public JwtAuthenticationFilter(TokenIssuerPort tokenIssuerPort) {
        this.tokenIssuerPort = tokenIssuerPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                TokenIssuerPort.TokenClaims claims = tokenIssuerPort.validate(token);
                var authority = new SimpleGrantedAuthority("ROLE_" + claims.role().name());
                var authentication = new UsernamePasswordAuthenticationToken(claims.username(), null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
