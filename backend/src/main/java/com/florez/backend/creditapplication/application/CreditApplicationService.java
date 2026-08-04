package com.florez.backend.creditapplication.application;

import com.florez.backend.common.domain.DuplicateResourceException;
import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.in.CreateCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.in.DeleteCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.in.GetCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.in.ListCreditApplicationsUseCase;
import com.florez.backend.creditapplication.domain.port.in.UpdateCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public final class CreditApplicationService implements
        CreateCreditApplicationUseCase,
        UpdateCreditApplicationUseCase,
        GetCreditApplicationUseCase,
        ListCreditApplicationsUseCase,
        DeleteCreditApplicationUseCase {

    private final CreditApplicationRepositoryPort repositoryPort;

    public CreditApplicationService(CreditApplicationRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public CreditApplication create(CreditApplication newApplication) {
        if (repositoryPort.existsByDocumentId(newApplication.getDocumentId())) {
            throw new DuplicateResourceException(
                    "Ya existe una solicitud con documentId '" + newApplication.getDocumentId() + "'");
        }
        return repositoryPort.save(newApplication);
    }

    @Override
    public CreditApplication update(UUID id, CreditApplication updatedData) {
        CreditApplication existing = findExisting(id);

        boolean documentIdChanged = !existing.getDocumentId().equals(updatedData.getDocumentId());
        if (documentIdChanged && repositoryPort.existsByDocumentId(updatedData.getDocumentId())) {
            throw new DuplicateResourceException(
                    "Ya existe una solicitud con documentId '" + updatedData.getDocumentId() + "'");
        }

        existing.applyUpdate(
                updatedData.getApplicantFullName(),
                updatedData.getDocumentId(),
                updatedData.getBirthDate(),
                updatedData.getMonthlyIncome(),
                updatedData.getRequestedAmount(),
                updatedData.getLoanTermMonths(),
                updatedData.getEmploymentYears(),
                updatedData.getExistingMonthlyDebt(),
                updatedData.getNumberOfDependents());

        return repositoryPort.save(existing);
    }

    @Override
    public CreditApplication getById(UUID id) {
        return findExisting(id);
    }

    @Override
    public Page<CreditApplication> list(Pageable pageable) {
        return repositoryPort.findAllNotDeleted(pageable);
    }

    @Override
    public void delete(UUID id) {
        CreditApplication existing = findExisting(id);
        existing.softDelete();
        repositoryPort.save(existing);
    }

    private CreditApplication findExisting(UUID id) {
        return repositoryPort.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una solicitud de crédito con id " + id));
    }
}
