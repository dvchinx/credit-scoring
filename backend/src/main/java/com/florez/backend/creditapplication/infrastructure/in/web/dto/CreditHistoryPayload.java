package com.florez.backend.creditapplication.infrastructure.in.web.dto;

import com.florez.backend.creditapplication.domain.model.CreditHistory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreditHistoryPayload(
        @NotNull(message = "la utilización de líneas rotativas es obligatoria")
        @PositiveOrZero(message = "la utilización de líneas rotativas no puede ser negativa") Double revolvingUtilization,
        @NotNull(message = "el número de líneas de crédito abiertas es obligatorio")
        @PositiveOrZero(message = "el número de líneas de crédito abiertas no puede ser negativo") Integer openCreditLines,
        @NotNull(message = "el número de créditos hipotecarios es obligatorio")
        @PositiveOrZero(message = "el número de créditos hipotecarios no puede ser negativo") Integer realEstateLoans,
        @NotNull(message = "las moras de 30-59 días son obligatorias")
        @PositiveOrZero(message = "las moras de 30-59 días no pueden ser negativas") Integer latePayments30To59Days,
        @NotNull(message = "las moras de 60-89 días son obligatorias")
        @PositiveOrZero(message = "las moras de 60-89 días no pueden ser negativas") Integer latePayments60To89Days,
        @NotNull(message = "las moras de 90 días o más son obligatorias")
        @PositiveOrZero(message = "las moras de 90 días o más no pueden ser negativas") Integer latePayments90DaysOrMore) {

    public CreditHistory toDomain() {
        return new CreditHistory(
                revolvingUtilization,
                openCreditLines,
                realEstateLoans,
                latePayments30To59Days,
                latePayments60To89Days,
                latePayments90DaysOrMore);
    }

    public static CreditHistoryPayload from(CreditHistory creditHistory) {
        return new CreditHistoryPayload(
                creditHistory.revolvingUtilization(),
                creditHistory.openCreditLines(),
                creditHistory.realEstateLoans(),
                creditHistory.latePayments30To59Days(),
                creditHistory.latePayments60To89Days(),
                creditHistory.latePayments90DaysOrMore());
    }
}
