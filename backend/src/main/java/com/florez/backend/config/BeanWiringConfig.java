package com.florez.backend.config;

import com.florez.backend.auth.application.AuthService;
import com.florez.backend.auth.domain.port.out.PasswordHasherPort;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.auth.domain.port.out.UserRepositoryPort;
import com.florez.backend.creditapplication.application.CreditApplicationService;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los servicios de la capa "application" son POJOs sin anotaciones de Spring (mantienen el
 * dominio libre del framework). Esta clase es el único punto donde se conectan con los
 * adaptadores concretos de los ports de salida.
 */
@Configuration
public class BeanWiringConfig {

    @Bean
    AuthService authService(
            UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort, TokenIssuerPort tokenIssuerPort) {
        return new AuthService(userRepositoryPort, passwordHasherPort, tokenIssuerPort);
    }

    @Bean
    CreditApplicationService creditApplicationService(CreditApplicationRepositoryPort repositoryPort) {
        return new CreditApplicationService(repositoryPort);
    }
}
