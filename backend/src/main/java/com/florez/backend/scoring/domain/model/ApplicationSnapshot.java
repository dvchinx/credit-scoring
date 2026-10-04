package com.florez.backend.scoring.domain.model;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Copia de los datos de la solicitud en el momento de la evaluación. La solicitud puede
 * editarse después; la decisión conserva lo que realmente se evaluó.
 */
public record ApplicationSnapshot(
        String documentId,
        LocalDate birthDate,
        BigDecimal monthlyIncome,
        BigDecimal requestedAmount,
        Integer loanTermMonths,
        Double employmentYears,
        BigDecimal existingMonthlyDebt,
        Integer numberOfDependents,
        CreditHistory creditHistory) {

    public static ApplicationSnapshot of(CreditApplication application) {
        return new ApplicationSnapshot(
                application.getDocumentId(),
                application.getBirthDate(),
                application.getMonthlyIncome(),
                application.getRequestedAmount(),
                application.getLoanTermMonths(),
                application.getEmploymentYears(),
                application.getExistingMonthlyDebt(),
                application.getNumberOfDependents(),
                application.getCreditHistory());
    }
}
