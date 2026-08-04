package com.florez.backend.auth.infrastructure.in.web;

import com.florez.backend.auth.domain.model.User;
import com.florez.backend.auth.domain.port.in.AuthenticateUserUseCase;
import com.florez.backend.auth.domain.port.in.AuthenticateUserUseCase.AuthenticatedSession;
import com.florez.backend.auth.domain.port.in.CreateUserUseCase;
import com.florez.backend.auth.infrastructure.in.web.dto.CreateUserRequest;
import com.florez.backend.auth.infrastructure.in.web.dto.LoginRequest;
import com.florez.backend.auth.infrastructure.in.web.dto.LoginResponse;
import com.florez.backend.auth.infrastructure.in.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final CreateUserUseCase createUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase, CreateUserUseCase createUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.createUserUseCase = createUserUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticatedSession session = authenticateUserUseCase.authenticate(request.username(), request.password());
        return ResponseEntity.ok(new LoginResponse(session.token(), session.tokenType(), session.expiresInMs()));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User created = createUserUseCase.createUser(request.username(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }
}
