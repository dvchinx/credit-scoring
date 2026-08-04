package com.florez.backend.auth.infrastructure.out.persistence;

import com.florez.backend.auth.domain.model.User;

final class UserMapper {

    private UserMapper() {
    }

    static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCreatedAt());
    }

    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(user.getUsername(), user.getPasswordHash(), user.getRole());
    }
}
