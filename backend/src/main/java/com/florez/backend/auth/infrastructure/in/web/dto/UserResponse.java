package com.florez.backend.auth.infrastructure.in.web.dto;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.model.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }
}
