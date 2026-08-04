package com.florez.backend.auth.infrastructure.out.security;

import com.florez.backend.auth.domain.model.Role;
import com.florez.backend.auth.domain.port.in.CreateUserUseCase;
import com.florez.backend.auth.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario ADMIN al arrancar la aplicación, únicamente si la tabla de usuarios
 * está vacía. Evita tener que hardcodear credenciales: se leen de variables de entorno.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final CreateUserUseCase createUserUseCase;
    private final UserRepositoryPort userRepositoryPort;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrapRunner(
            CreateUserUseCase createUserUseCase,
            UserRepositoryPort userRepositoryPort,
            @Value("${admin.bootstrap.username}") String adminUsername,
            @Value("${admin.bootstrap.password}") String adminPassword) {
        this.createUserUseCase = createUserUseCase;
        this.userRepositoryPort = userRepositoryPort;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepositoryPort.count() > 0) {
            return;
        }

        createUserUseCase.createUser(adminUsername, adminPassword, Role.ADMIN);
        log.info("Usuario ADMIN inicial '{}' creado. Cambia su contraseña en producción.", adminUsername);
    }
}
