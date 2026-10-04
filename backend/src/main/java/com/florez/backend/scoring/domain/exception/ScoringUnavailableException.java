package com.florez.backend.scoring.domain.exception;

import com.florez.backend.common.domain.ServiceUnavailableException;

/** El servicio de ML no pudo devolver un score o una explicación válidos. */
public class ScoringUnavailableException extends ServiceUnavailableException {

    public ScoringUnavailableException(String message) {
        super(message);
    }

    public ScoringUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
