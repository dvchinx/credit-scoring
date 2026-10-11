package com.florez.backend.common.domain;

/** La operación es válida en forma, pero el estado actual del sistema no permite realizarla. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
