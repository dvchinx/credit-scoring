package com.florez.backend.config;

import com.florez.backend.auth.application.AuthService;
import com.florez.backend.auth.domain.port.out.PasswordHasherPort;
import com.florez.backend.auth.domain.port.out.TokenIssuerPort;
import com.florez.backend.auth.domain.port.out.UserRepositoryPort;
import com.florez.backend.common.application.TransactionRunner;
import com.florez.backend.creditapplication.application.CreditApplicationService;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import com.florez.backend.modelregistry.application.ModelRegistryService;
import com.florez.backend.modelregistry.domain.port.out.ModelActivationRepositoryPort;
import com.florez.backend.modelregistry.domain.port.out.ModelArtifactCatalogPort;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import com.florez.backend.scoring.application.CreditDecisionService;
import com.florez.backend.scoring.domain.model.DecisionPolicy;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.port.out.CreditDecisionRepositoryPort;
import com.florez.backend.scoring.domain.port.out.ScoringModelPort;
import com.florez.backend.scoring.infrastructure.config.CreditPolicyProperties;
import com.florez.backend.whatif.application.WhatIfService;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort;
import com.florez.backend.whatif.domain.port.out.WhatIfSimulationRepositoryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
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

    /**
     * Resolución de microsegundos, la misma que TIMESTAMPTZ en Postgres: un timestamp devuelto al
     * crear una decisión o activación es idéntico al que se leerá después desde la base de datos.
     */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemDefaultZone(), Duration.of(1, ChronoUnit.MICROS));
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
    ModelRegistryService modelRegistryService(
            ModelVersionRepositoryPort versionRepositoryPort,
            ModelActivationRepositoryPort activationRepositoryPort,
            ModelArtifactCatalogPort catalogPort,
            Clock clock) {
        // El registry solo acepta modelos cuyas variables de entrada el backend sabe calcular.
        return new ModelRegistryService(
                versionRepositoryPort, activationRepositoryPort, catalogPort, Set.copyOf(ModelFeatures.FEATURE_NAMES), clock);
    }

    @Bean
    CreditDecisionService creditDecisionService(
            CreditApplicationRepositoryPort applicationRepositoryPort,
            CreditDecisionRepositoryPort decisionRepositoryPort,
            ScoringModelPort scoringModelPort,
            ModelRegistryService modelRegistryService,
            DecisionPolicy decisionPolicy,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new CreditDecisionService(
                applicationRepositoryPort,
                decisionRepositoryPort,
                scoringModelPort,
                modelRegistryService,
                decisionPolicy,
                transactionRunner,
                clock);
    }

    /** Misma política para decisiones reales y simulaciones: un what-if predice exactamente lo que se decidiría. */
    @Bean
    DecisionPolicy decisionPolicy(CreditPolicyProperties creditPolicyProperties) {
        // toDomain() valida los umbrales: una política mal configurada impide arrancar la aplicación.
        return creditPolicyProperties.toDomain();
    }

    @Bean
    WhatIfService whatIfService(
            CreditApplicationRepositoryPort applicationRepositoryPort,
            WhatIfSimulationRepositoryPort simulationRepositoryPort,
            WhatIfModelPort whatIfModelPort,
            ModelRegistryService modelRegistryService,
            DecisionPolicy decisionPolicy,
            Clock clock) {
        return new WhatIfService(
                applicationRepositoryPort, simulationRepositoryPort, whatIfModelPort, modelRegistryService, decisionPolicy, clock);
    }
}
