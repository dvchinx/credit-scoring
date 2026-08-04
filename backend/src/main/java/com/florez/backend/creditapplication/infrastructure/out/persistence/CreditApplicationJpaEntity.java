package com.florez.backend.creditapplication.infrastructure.out.persistence;

import com.florez.backend.common.infrastructure.persistence.BaseJpaAuditEntity;
import com.florez.backend.creditapplication.domain.model.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "credit_applications")
public class CreditApplicationJpaEntity extends BaseJpaAuditEntity {

    @Column(name = "applicant_full_name", nullable = false)
    private String applicantFullName;

    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "monthly_income", nullable = false)
    private BigDecimal monthlyIncome;

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "loan_term_months", nullable = false)
    private Integer loanTermMonths;

    @Column(name = "employment_years", nullable = false)
    private Double employmentYears;

    @Column(name = "existing_monthly_debt", nullable = false)
    private BigDecimal existingMonthlyDebt;

    @Column(name = "number_of_dependents", nullable = false)
    private Integer numberOfDependents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CreditApplicationJpaEntity() {
        // requerido por JPA
    }

    public CreditApplicationJpaEntity(
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
            Instant deletedAt) {
        this.applicantFullName = applicantFullName;
        this.documentId = documentId;
        this.birthDate = birthDate;
        this.monthlyIncome = monthlyIncome;
        this.requestedAmount = requestedAmount;
        this.loanTermMonths = loanTermMonths;
        this.employmentYears = employmentYears;
        this.existingMonthlyDebt = existingMonthlyDebt;
        this.numberOfDependents = numberOfDependents;
        this.status = status;
        this.createdBy = createdBy;
        this.deletedAt = deletedAt;
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

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void updateFrom(
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
            Instant deletedAt) {
        this.applicantFullName = applicantFullName;
        this.documentId = documentId;
        this.birthDate = birthDate;
        this.monthlyIncome = monthlyIncome;
        this.requestedAmount = requestedAmount;
        this.loanTermMonths = loanTermMonths;
        this.employmentYears = employmentYears;
        this.existingMonthlyDebt = existingMonthlyDebt;
        this.numberOfDependents = numberOfDependents;
        this.status = status;
        this.deletedAt = deletedAt;
    }
}
