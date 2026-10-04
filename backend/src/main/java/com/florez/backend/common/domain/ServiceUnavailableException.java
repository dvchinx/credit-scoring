package com.florez.backend.common.domain;

/** Una dependencia externa necesaria para completar la operación no está disponible. */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
