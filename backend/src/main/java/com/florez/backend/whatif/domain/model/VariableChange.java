package com.florez.backend.whatif.domain.model;

import java.math.BigDecimal;

/** Una variable de la solicitud con su valor real y el valor simulado. */
public record VariableChange(String variable, BigDecimal baselineValue, BigDecimal scenarioValue) {
}
