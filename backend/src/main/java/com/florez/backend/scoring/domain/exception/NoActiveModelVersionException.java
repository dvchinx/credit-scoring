package com.florez.backend.scoring.domain.exception;

import com.florez.backend.common.domain.BusinessRuleException;

/** No se puede evaluar: ninguna versión de modelo ha sido activada en el model registry. */
public class NoActiveModelVersionException extends BusinessRuleException {

    public NoActiveModelVersionException() {
        super("No hay ninguna versión de modelo activa: registra y activa una en /model-versions y /model-activations");
    }
}
