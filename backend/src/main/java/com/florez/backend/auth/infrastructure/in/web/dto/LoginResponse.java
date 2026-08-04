package com.florez.backend.auth.infrastructure.in.web.dto;

public record LoginResponse(String token, String tokenType, long expiresInMs) {
}
