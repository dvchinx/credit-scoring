package com.florez.backend.auth.application;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.model.User;
import com.florez.backend.auth.domain.port.in.AuthenticateUserUseCase;
import com.florez.backend.auth.domain.port.in.CreateUserUseCase;
import com.florez.backend.auth.domain.port.out.PasswordHasherPort;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.auth.domain.port.out.UserRepositoryPort;
import com.florez.backend.common.domain.DuplicateResourceException;
import com.florez.backend.common.domain.InvalidCredentialsException;

public final class AuthService implements AuthenticateUserUseCase, CreateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenIssuerPort tokenIssuerPort;

    public AuthService(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort, TokenIssuerPort tokenIssuerPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenIssuerPort = tokenIssuerPort;
    }

    @Override
    public AuthenticatedSession authenticate(String username, String rawPassword) {
        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña inválidos"));

        if (!passwordHasherPort.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Usuario o contraseña inválidos");
        }

        TokenIssuerPort.IssuedToken issuedToken = tokenIssuerPort.issue(user.getUsername(), user.getRole());
        return new AuthenticatedSession(issuedToken.token(), issuedToken.tokenType(), issuedToken.expiresInMs());
    }

    @Override
    public User createUser(String username, String rawPassword, Role role) {
        if (userRepositoryPort.existsByUsername(username)) {
            throw new DuplicateResourceException("Ya existe un usuario con username '" + username + "'");
        }

        User newUser = User.createNew(username, passwordHasherPort.hash(rawPassword), role);
        return userRepositoryPort.save(newUser);
    }
}
