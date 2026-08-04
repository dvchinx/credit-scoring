package com.florez.backend.creditapplication.infrastructure.in.web.dto;

import com.florez.backend.creditapplication.domain.model.ApplicationStatus;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreditApplicationResponse(
        UUID id,
        String applicantFullName,
        String documentId,
        LocalDate birthDate,
        BigDecimal monthlyIncome,
        BigDecimal requestedAmount,
        Integer loanTermMonths,
        Double employmentYears,
        BigDecimal existingMonthlyDebt,
        Integer numberOfDependents,
        ApplicationStatus status,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public static CreditApplicationResponse from(CreditApplication application) {
        return new CreditApplicationResponse(
                application.getId(),
                application.getApplicantFullName(),
                application.getDocumentId(),
                application.getBirthDate(),
                application.getMonthlyIncome(),
                application.getRequestedAmount(),
                application.getLoanTermMonths(),
                application.getEmploymentYears(),
                application.getExistingMonthlyDebt(),
                application.getNumberOfDependents(),
                application.getStatus(),
                application.getCreatedBy(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }
}
