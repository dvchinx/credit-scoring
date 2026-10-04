package com.florez.backend.scoring.domain.model;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrada exacta que recibe el modelo de riesgo, con los mismos nombres canónicos de feature
 * que usa el servicio de ML (ver ml-service/training/preprocessing.py). Se persiste tal cual
 * en cada decisión para poder reproducir el score meses después.
 */
public record ModelFeatures(
        double revolvingUtilizationOfUnsecuredLines,
        int age,
        int numberOfTime30To59DaysPastDueNotWorse,
        double debtRatio,
        double monthlyIncome,
        int numberOfOpenCreditLinesAndLoans,
        int numberOfTimes90DaysLate,
        int numberRealEstateLoansOrLines,
        int numberOfTime60To89DaysPastDueNotWorse,
        int numberOfDependents) {

    /**
     * Traduce una solicitud a las variables del modelo:
     * <ul>
     *   <li>{@code age}: años cumplidos a la fecha de evaluación.</li>
     *   <li>{@code debt_ratio}: (deuda mensual existente + cuota estimada del crédito solicitado) /
     *       ingreso mensual. La cuota se estima como monto / plazo, sin intereses, para medir el
     *       endeudamiento que tendría el solicitante si se aprueba el crédito.</li>
     *   <li>Historial de mora, líneas abiertas e hipotecas: directo de {@link CreditHistory}.</li>
     * </ul>
     */
    public static ModelFeatures from(CreditApplication application, LocalDate evaluationDate) {
        CreditHistory history = application.getCreditHistory();
        return new ModelFeatures(
                history.revolvingUtilization(),
                Period.between(application.getBirthDate(), evaluationDate).getYears(),
                history.latePayments30To59Days(),
                debtRatioIncludingNewLoan(application),
                application.getMonthlyIncome().doubleValue(),
                history.openCreditLines(),
                history.latePayments90DaysOrMore(),
                history.realEstateLoans(),
                history.latePayments60To89Days(),
                application.getNumberOfDependents());
    }

    private static double debtRatioIncludingNewLoan(CreditApplication application) {
        BigDecimal estimatedInstallment = application.getRequestedAmount()
                .divide(BigDecimal.valueOf(application.getLoanTermMonths()), MathContext.DECIMAL64);
        return application.getExistingMonthlyDebt()
                .add(estimatedInstallment)
                .divide(application.getMonthlyIncome(), MathContext.DECIMAL64)
                .doubleValue();
    }

    /** Features con sus nombres canónicos del modelo, en el orden de entrenamiento. */
    public Map<String, Number> asFeatureMap() {
        Map<String, Number> features = new LinkedHashMap<>();
        features.put("revolving_utilization_of_unsecured_lines", revolvingUtilizationOfUnsecuredLines);
        features.put("age", age);
        features.put("number_of_time_30_59_days_past_due_not_worse", numberOfTime30To59DaysPastDueNotWorse);
        features.put("debt_ratio", debtRatio);
        features.put("monthly_income", monthlyIncome);
        features.put("number_of_open_credit_lines_and_loans", numberOfOpenCreditLinesAndLoans);
        features.put("number_of_times_90_days_late", numberOfTimes90DaysLate);
        features.put("number_real_estate_loans_or_lines", numberRealEstateLoansOrLines);
        features.put("number_of_time_60_89_days_past_due_not_worse", numberOfTime60To89DaysPastDueNotWorse);
        features.put("number_of_dependents", numberOfDependents);
        return features;
    }
}
