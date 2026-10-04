package com.florez.backend.creditapplication.domain.model;

import java.util.Objects;

/**
 * Historial crediticio del solicitante, tal como lo reportaría un buró de crédito. Son las
 * variables de comportamiento de pago que el modelo de riesgo necesita y que no se derivan
 * de los datos básicos de la solicitud.
 *
 * @param revolvingUtilization  saldo de líneas rotativas no garantizadas / límite total (0.3 = 30%)
 * @param openCreditLines        número de créditos y líneas abiertas
 * @param realEstateLoans        número de créditos hipotecarios o líneas con garantía inmobiliaria
 * @param latePayments30To59Days veces con mora de 30-59 días en los últimos 2 años
 * @param latePayments60To89Days veces con mora de 60-89 días en los últimos 2 años
 * @param latePayments90DaysOrMore veces con mora de 90 días o más
 */
public record CreditHistory(
        Double revolvingUtilization,
        Integer openCreditLines,
        Integer realEstateLoans,
        Integer latePayments30To59Days,
        Integer latePayments60To89Days,
        Integer latePayments90DaysOrMore) {

    public CreditHistory {
        Objects.requireNonNull(revolvingUtilization, "revolvingUtilization");
        Objects.requireNonNull(openCreditLines, "openCreditLines");
        Objects.requireNonNull(realEstateLoans, "realEstateLoans");
        Objects.requireNonNull(latePayments30To59Days, "latePayments30To59Days");
        Objects.requireNonNull(latePayments60To89Days, "latePayments60To89Days");
        Objects.requireNonNull(latePayments90DaysOrMore, "latePayments90DaysOrMore");
    }
}
