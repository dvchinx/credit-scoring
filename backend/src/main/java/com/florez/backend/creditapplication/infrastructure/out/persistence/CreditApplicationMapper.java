package com.florez.backend.creditapplication.infrastructure.out.persistence;

import com.florez.backend.creditapplication.domain.model.CreditApplication;

final class CreditApplicationMapper {

    private CreditApplicationMapper() {
    }

    static CreditApplication toDomain(CreditApplicationJpaEntity entity) {
        return CreditApplication.reconstitute(
                entity.getId(),
                entity.getApplicantFullName(),
                entity.getDocumentId(),
                entity.getBirthDate(),
                entity.getMonthlyIncome(),
                entity.getRequestedAmount(),
                entity.getLoanTermMonths(),
                entity.getEmploymentYears(),
                entity.getExistingMonthlyDebt(),
                entity.getNumberOfDependents(),
                entity.getCreditHistory(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt());
    }

    static CreditApplicationJpaEntity toNewEntity(CreditApplication application) {
        return new CreditApplicationJpaEntity(
                application.getApplicantFullName(),
                application.getDocumentId(),
                application.getBirthDate(),
                application.getMonthlyIncome(),
                application.getRequestedAmount(),
                application.getLoanTermMonths(),
                application.getEmploymentYears(),
                application.getExistingMonthlyDebt(),
                application.getNumberOfDependents(),
                application.getCreditHistory(),
                application.getStatus(),
                application.getCreatedBy(),
                application.getDeletedAt());
    }

    static void copyOnto(CreditApplication application, CreditApplicationJpaEntity entity) {
        entity.updateFrom(
                application.getApplicantFullName(),
                application.getDocumentId(),
                application.getBirthDate(),
                application.getMonthlyIncome(),
                application.getRequestedAmount(),
                application.getLoanTermMonths(),
                application.getEmploymentYears(),
                application.getExistingMonthlyDebt(),
                application.getNumberOfDependents(),
                application.getCreditHistory(),
                application.getStatus(),
                application.getDeletedAt());
    }
}
