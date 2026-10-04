package com.florez.backend.creditapplication.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CreditApplication {

    private final UUID id;
    private String applicantFullName;
    private String documentId;
    private LocalDate birthDate;
    private BigDecimal monthlyIncome;
    private BigDecimal requestedAmount;
    private Integer loanTermMonths;
    private Double employmentYears;
    private BigDecimal existingMonthlyDebt;
    private Integer numberOfDependents;
    private CreditHistory creditHistory;
    private ApplicationStatus status;
    private final String createdBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private CreditApplication(
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
            CreditHistory creditHistory,
            ApplicationStatus status,
            String createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.applicantFullName = applicantFullName;
        this.documentId = documentId;
        this.birthDate = birthDate;
        this.monthlyIncome = monthlyIncome;
        this.requestedAmount = requestedAmount;
        this.loanTermMonths = loanTermMonths;
        this.employmentYears = employmentYears;
        this.existingMonthlyDebt = existingMonthlyDebt;
        this.numberOfDependents = numberOfDependents;
        this.creditHistory = creditHistory;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static CreditApplication createNew(
            String applicantFullName,
            String documentId,
            LocalDate birthDate,
            BigDecimal monthlyIncome,
            BigDecimal requestedAmount,
            Integer loanTermMonths,
            Double employmentYears,
            BigDecimal existingMonthlyDebt,
            Integer numberOfDependents,
            CreditHistory creditHistory,
            String createdBy) {
        return new CreditApplication(
                null,
                applicantFullName,
                documentId,
                birthDate,
                monthlyIncome,
                requestedAmount,
                loanTermMonths,
                employmentYears,
                existingMonthlyDebt,
                numberOfDependents,
                creditHistory,
                ApplicationStatus.PENDING,
                createdBy,
                null,
                null,
                null);
    }

    public static CreditApplication reconstitute(
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
            CreditHistory creditHistory,
            ApplicationStatus status,
            String createdBy,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        return new CreditApplication(
                id,
                applicantFullName,
                documentId,
                birthDate,
                monthlyIncome,
                requestedAmount,
                loanTermMonths,
                employmentYears,
                existingMonthlyDebt,
                numberOfDependents,
                creditHistory,
                status,
                createdBy,
                createdAt,
                updatedAt,
                deletedAt);
    }

    public void applyUpdate(
            String applicantFullName,
            String documentId,
            LocalDate birthDate,
            BigDecimal monthlyIncome,
            BigDecimal requestedAmount,
            Integer loanTermMonths,
            Double employmentYears,
            BigDecimal existingMonthlyDebt,
            Integer numberOfDependents,
            CreditHistory creditHistory) {
        this.applicantFullName = applicantFullName;
        this.documentId = documentId;
        this.birthDate = birthDate;
        this.monthlyIncome = monthlyIncome;
        this.requestedAmount = requestedAmount;
        this.loanTermMonths = loanTermMonths;
        this.employmentYears = employmentYears;
        this.existingMonthlyDebt = existingMonthlyDebt;
        this.numberOfDependents = numberOfDependents;
        this.creditHistory = creditHistory;
    }

    /**
     * Refleja en la solicitud el resultado de su última evaluación. El detalle completo
     * (score, explicación, versión de modelo) vive en la decisión, no aquí.
     */
    public void recordDecisionOutcome(ApplicationStatus outcome) {
        if (outcome == ApplicationStatus.PENDING || outcome == ApplicationStatus.IN_REVIEW) {
            throw new IllegalArgumentException("Un resultado de decisión no puede ser " + outcome);
        }
        this.status = outcome;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getApplicantFullName() {
        return applicantFullName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public Integer getLoanTermMonths() {
        return loanTermMonths;
    }

    public Double getEmploymentYears() {
        return employmentYears;
    }

    public BigDecimal getExistingMonthlyDebt() {
        return existingMonthlyDebt;
    }

    public Integer getNumberOfDependents() {
        return numberOfDependents;
    }

    public CreditHistory getCreditHistory() {
        return creditHistory;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
