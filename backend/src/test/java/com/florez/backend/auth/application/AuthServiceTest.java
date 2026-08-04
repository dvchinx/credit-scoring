package com.florez.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.model.User;
import com.florez.backend.auth.domain.port.in.AuthenticateUserUseCase.AuthenticatedSession;
import com.florez.backend.auth.domain.port.out.PasswordHasherPort;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.auth.domain.port.out.UserRepositoryPort;
import com.florez.backend.common.domain.DuplicateResourceException;
import com.florez.backend.common.domain.InvalidCredentialsException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordHasherPort passwordHasherPort;

    @Mock
    private TokenIssuerPort tokenIssuerPort;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepositoryPort, passwordHasherPort, tokenIssuerPort);
    }

    @Test
    void authenticate_conCredencialesValidas_devuelveToken() {
        User user = User.reconstitute(UUID.randomUUID(), "analyst1", "hashed", Role.ANALYST, null);
        when(userRepositoryPort.findByUsername("analyst1")).thenReturn(Optional.of(user));
        when(passwordHasherPort.matches("secret123", "hashed")).thenReturn(true);
        when(tokenIssuerPort.issue("analyst1", Role.ANALYST))
                .thenReturn(new TokenIssuerPort.IssuedToken("jwt-token", "Bearer", 3_600_000L));

        AuthenticatedSession session = authService.authenticate("analyst1", "secret123");

        assertThat(session.token()).isEqualTo("jwt-token");
        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.expiresInMs()).isEqualTo(3_600_000L);
    }

    @Test
    void authenticate_conUsuarioInexistente_lanzaInvalidCredentials() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("ghost", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_conPasswordIncorrecto_lanzaInvalidCredentials() {
        User user = User.reconstitute(UUID.randomUUID(), "analyst1", "hashed", Role.ANALYST, null);
        when(userRepositoryPort.findByUsername("analyst1")).thenReturn(Optional.of(user));
        when(passwordHasherPort.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate("analyst1", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void createUser_conUsernameNuevo_creaYPersisteElUsuario() {
        when(userRepositoryPort.existsByUsername("newuser")).thenReturn(false);
        when(passwordHasherPort.hash("secret123")).thenReturn("hashed-secret");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = authService.createUser("newuser", "secret123", Role.ANALYST);

        assertThat(created.getUsername()).isEqualTo("newuser");
        assertThat(created.getPasswordHash()).isEqualTo("hashed-secret");
        assertThat(created.getRole()).isEqualTo(Role.ANALYST);
    }

    @Test
    void createUser_conUsernameExistente_lanzaDuplicateResource() {
        when(userRepositoryPort.existsByUsername("dup")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser("dup", "secret123", Role.ANALYST))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
