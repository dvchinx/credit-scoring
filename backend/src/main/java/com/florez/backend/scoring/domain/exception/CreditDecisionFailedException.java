package com.florez.backend.scoring.domain.exception;

import com.florez.backend.common.domain.ServiceUnavailableException;
import java.util.UUID;

/** La evaluación no pudo completarse; el intento quedó registrado como decisión FAILED. */
public class CreditDecisionFailedException extends ServiceUnavailableException {

    private final UUID failedDecisionId;

    public CreditDecisionFailedException(UUID failedDecisionId, String reason) {
        super("No se pudo evaluar la solicitud: " + reason + " (intento registrado como decisión " + failedDecisionId + ")");
        this.failedDecisionId = failedDecisionId;
    }

    public UUID getFailedDecisionId() {
        return failedDecisionId;
    }
}
