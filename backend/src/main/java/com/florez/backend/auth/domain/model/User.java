package com.florez.backend.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Instant createdAt;

    private User(UUID id, String username, String passwordHash, Role role, Instant createdAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "username no puede ser nulo");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash no puede ser nulo");
        this.role = Objects.requireNonNull(role, "role no puede ser nulo");
        this.createdAt = createdAt;
    }

    public static User createNew(String username, String passwordHash, Role role) {
        return new User(null, username, passwordHash, role, null);
    }

    public static User reconstitute(UUID id, String username, String passwordHash, Role role, Instant createdAt) {
        return new User(id, username, passwordHash, role, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
