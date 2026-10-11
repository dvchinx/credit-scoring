package com.florez.backend.whatif.infrastructure.in.web.dto;

import com.florez.backend.whatif.domain.model.ApplicationChanges;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

/** Cambios a simular; un campo ausente o {@code null} se mantiene con el valor real de la solicitud. */
public record WhatIfRequest(
        @Positive(message = "el ingreso mensual debe ser positivo") BigDecimal monthlyIncome,
        @Positive(message = "el monto solicitado debe ser positivo") BigDecimal requestedAmount,
        @Positive(message = "el plazo en meses debe ser positivo") Integer loanTermMonths,
        @PositiveOrZero(message = "la deuda mensual existente no puede ser negativa") BigDecimal existingMonthlyDebt,
        @PositiveOrZero(message = "el número de dependientes no puede ser negativo") Integer numberOfDependents,
        @Valid CreditHistoryChanges creditHistory) {

    public record CreditHistoryChanges(
            @PositiveOrZero(message = "la utilización de líneas rotativas no puede ser negativa") Double revolvingUtilization,
            @PositiveOrZero(message = "el número de líneas abiertas no puede ser negativo") Integer openCreditLines,
            @PositiveOrZero(message = "el número de créditos hipotecarios no puede ser negativo") Integer realEstateLoans,
            @PositiveOrZero(message = "las moras de 30-59 días no pueden ser negativas") Integer latePayments30To59Days,
            @PositiveOrZero(message = "las moras de 60-89 días no pueden ser negativas") Integer latePayments60To89Days,
            @PositiveOrZero(message = "las moras de 90 días o más no pueden ser negativas") Integer latePayments90DaysOrMore) {
    }

    @AssertTrue(message = "la simulación debe cambiar al menos una variable")
    public boolean isAnyVariableChanged() {
        Stream<Object> history = creditHistory == null
                ? Stream.empty()
                : Stream.of(creditHistory.revolvingUtilization(), creditHistory.openCreditLines(),
                        creditHistory.realEstateLoans(), creditHistory.latePayments30To59Days(),
                        creditHistory.latePayments60To89Days(), creditHistory.latePayments90DaysOrMore());
        return Stream.concat(
                        Stream.of(monthlyIncome, requestedAmount, loanTermMonths, existingMonthlyDebt, numberOfDependents),
                        history)
                .anyMatch(Objects::nonNull);
    }

    public ApplicationChanges toDomain() {
        CreditHistoryChanges history = creditHistory != null
                ? creditHistory
                : new CreditHistoryChanges(null, null, null, null, null, null);
        return new ApplicationChanges(
                monthlyIncome,
                requestedAmount,
                loanTermMonths,
                existingMonthlyDebt,
                numberOfDependents,
                history.revolvingUtilization(),
                history.openCreditLines(),
                history.realEstateLoans(),
                history.latePayments30To59Days(),
                history.latePayments60To89Days(),
                history.latePayments90DaysOrMore());
    }
}
