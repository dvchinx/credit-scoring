package com.florez.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

/**
 * {@code VIA_DTO}: las páginas se serializan como {@code {content, page: {size, number, totalElements,
 * totalPages}}}, un contrato estable para los clientes en lugar de la estructura interna de PageImpl.
 */
@Configuration
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class JpaAuditingConfig {
}
