package com.florez.backend.modelregistry.domain.exception;

import com.florez.backend.common.domain.BusinessRuleException;

/** El servicio de ML reporta una versión con metadata incompleta o inválida (p. ej. entrenada antes de la Fase 4). */
public class InvalidModelArtifactException extends BusinessRuleException {

    public InvalidModelArtifactException(String message) {
        super(message);
    }
}
