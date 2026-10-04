package com.florez.backend.config;

import com.florez.backend.auth.application.AuthService;
import com.florez.backend.auth.domain.port.out.PasswordHasherPort;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.auth.domain.port.out.UserRepositoryPort;
import com.florez.backend.common.application.TransactionRunner;
import com.florez.backend.creditapplication.application.CreditApplicationService;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.scoring.application.CreditDecisionService;
import com.florez.backend.scoring.domain.port.out.CreditDecisionRepositoryPort;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import com.florez.backend.scoring.infrastructure.config.CreditPolicyProperties;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Los servicios de la capa "application" son POJOs sin anotaciones de Spring (mantienen el
 * dominio libre del framework). Esta clase es el único punto donde se conectan con los
 * adaptadores concretos de los ports de salida.
 */
@Configuration
@EnableConfigurationProperties(CreditPolicyProperties.class)
public class BeanWiringConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    AuthService authService(
            UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort, TokenIssuerPort tokenIssuerPort) {
        return new AuthService(userRepositoryPort, passwordHasherPort, tokenIssuerPort);
    }

    @Bean
    CreditApplicationService creditApplicationService(CreditApplicationRepositoryPort repositoryPort) {
        return new CreditApplicationService(repositoryPort);
    }

    @Bean
    CreditDecisionService creditDecisionService(
            CreditApplicationRepositoryPort applicationRepositoryPort,
            CreditDecisionRepositoryPort decisionRepositoryPort,
            ScoringModelPort scoringModelPort,
            CreditPolicyProperties creditPolicyProperties,
            TransactionRunner transactionRunner,
            Clock clock) {
        // toDomain() valida los umbrales: una política mal configurada impide arrancar la aplicación.
        return new CreditDecisionService(
                applicationRepositoryPort,
                decisionRepositoryPort,
                scoringModelPort,
                creditPolicyProperties.toDomain(),
                transactionRunner,
                clock);
    }
}
