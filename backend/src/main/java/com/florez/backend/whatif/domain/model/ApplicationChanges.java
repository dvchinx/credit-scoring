package com.florez.backend.whatif.domain.model;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Cambios hipotéticos sobre una solicitud. Solo se pueden simular variables financieras y de
 * historial crediticio (lo que un solicitante puede mejorar); un campo {@code null} significa
 * "sin cambio". La solicitud real nunca se modifica.
 */
public record ApplicationChanges(
        BigDecimal monthlyIncome,
        BigDecimal requestedAmount,
        Integer loanTermMonths,
        BigDecimal existingMonthlyDebt,
        Integer numberOfDependents,
        Double revolvingUtilization,
        Integer openCreditLines,
        Integer realEstateLoans,
        Integer latePayments30To59Days,
        Integer latePayments60To89Days,
        Integer latePayments90DaysOrMore) {

    public ApplicationChanges {
        if (Stream.of(monthlyIncome, requestedAmount, loanTermMonths, existingMonthlyDebt, numberOfDependents,
                revolvingUtilization, openCreditLines, realEstateLoans, latePayments30To59Days, latePayments60To89Days,
                latePayments90DaysOrMore).allMatch(value -> value == null)) {
            throw new IllegalArgumentException("Una simulación debe cambiar al menos una variable");
        }
        requirePositive("monthlyIncome", monthlyIncome);
        requirePositive("requestedAmount", requestedAmount);
        if (loanTermMonths != null && loanTermMonths <= 0) {
            throw new IllegalArgumentException("loanTermMonths debe ser positivo");
        }
        if (existingMonthlyDebt != null && existingMonthlyDebt.signum() < 0) {
            throw new IllegalArgumentException("existingMonthlyDebt no puede ser negativo");
        }
        if (revolvingUtilization != null && revolvingUtilization < 0) {
            throw new IllegalArgumentException("revolvingUtilization no puede ser negativo");
        }
        if (Stream.of(numberOfDependents, openCreditLines, realEstateLoans, latePayments30To59Days,
                latePayments60To89Days, latePayments90DaysOrMore).anyMatch(value -> value != null && value < 0)) {
            throw new IllegalArgumentException("Los contadores no pueden ser negativos");
        }
    }

    private static void requirePositive(String name, BigDecimal value) {
        if (value != null && value.signum() <= 0) {
            throw new IllegalArgumentException(name + " debe ser positivo");
        }
    }

    /** Copia en memoria de la solicitud con los cambios aplicados. No se persiste. */
    public CreditApplication applyTo(CreditApplication application) {
        CreditHistory history = application.getCreditHistory();
        return CreditApplication.reconstitute(
                application.getId(),
                application.getApplicantFullName(),
                application.getDocumentId(),
                application.getBirthDate(),
                orElse(monthlyIncome, application.getMonthlyIncome()),
                orElse(requestedAmount, application.getRequestedAmount()),
                orElse(loanTermMonths, application.getLoanTermMonths()),
                application.getEmploymentYears(),
                orElse(existingMonthlyDebt, application.getExistingMonthlyDebt()),
                orElse(numberOfDependents, application.getNumberOfDependents()),
                new CreditHistory(
                        orElse(revolvingUtilization, history.revolvingUtilization()),
                        orElse(openCreditLines, history.openCreditLines()),
                        orElse(realEstateLoans, history.realEstateLoans()),
                        orElse(latePayments30To59Days, history.latePayments30To59Days()),
                        orElse(latePayments60To89Days, history.latePayments60To89Days()),
                        orElse(latePayments90DaysOrMore, history.latePayments90DaysOrMore())),
                application.getStatus(),
                application.getCreatedBy(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                application.getDeletedAt());
    }

    /** Variables cambiadas, con su valor real y el simulado, en el orden en que se declaran. */
    public List<VariableChange> describe(CreditApplication application) {
        CreditHistory history = application.getCreditHistory();
        List<VariableChange> changes = new ArrayList<>();
        add(changes, "monthlyIncome", application.getMonthlyIncome(), monthlyIncome);
        add(changes, "requestedAmount", application.getRequestedAmount(), requestedAmount);
        add(changes, "loanTermMonths", application.getLoanTermMonths(), loanTermMonths);
        add(changes, "existingMonthlyDebt", application.getExistingMonthlyDebt(), existingMonthlyDebt);
        add(changes, "numberOfDependents", application.getNumberOfDependents(), numberOfDependents);
        add(changes, "creditHistory.revolvingUtilization", history.revolvingUtilization(), revolvingUtilization);
        add(changes, "creditHistory.openCreditLines", history.openCreditLines(), openCreditLines);
        add(changes, "creditHistory.realEstateLoans", history.realEstateLoans(), realEstateLoans);
        add(changes, "creditHistory.latePayments30To59Days", history.latePayments30To59Days(), latePayments30To59Days);
        add(changes, "creditHistory.latePayments60To89Days", history.latePayments60To89Days(), latePayments60To89Days);
        add(changes, "creditHistory.latePayments90DaysOrMore", history.latePayments90DaysOrMore(), latePayments90DaysOrMore);
        return changes;
    }

    private static void add(List<VariableChange> changes, String variable, Number baseline, Number scenario) {
        if (scenario != null) {
            changes.add(new VariableChange(variable, toDecimal(baseline), toDecimal(scenario)));
        }
    }

    private static BigDecimal toDecimal(Number value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static <T> T orElse(T change, T current) {
        return change != null ? change : current;
    }
}
