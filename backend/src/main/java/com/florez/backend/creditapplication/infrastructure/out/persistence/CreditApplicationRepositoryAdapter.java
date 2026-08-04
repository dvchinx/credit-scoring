package com.florez.backend.creditapplication.infrastructure.out.persistence;

import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class CreditApplicationRepositoryAdapter implements CreditApplicationRepositoryPort {

    private final SpringDataCreditApplicationRepository springDataRepository;

    public CreditApplicationRepositoryAdapter(SpringDataCreditApplicationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CreditApplication save(CreditApplication application) {
        CreditApplicationJpaEntity entity;
        if (application.getId() == null) {
            entity = CreditApplicationMapper.toNewEntity(application);
        } else {
            entity = springDataRepository.findById(application.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No existe una solicitud de crédito con id " + application.getId()));
            CreditApplicationMapper.copyOnto(application, entity);
        }
        return CreditApplicationMapper.toDomain(springDataRepository.save(entity));
    }

    @Override
    public Optional<CreditApplication> findByIdAndNotDeleted(UUID id) {
        return springDataRepository.findByIdAndDeletedAtIsNull(id).map(CreditApplicationMapper::toDomain);
    }

    @Override
    public Page<CreditApplication> findAllNotDeleted(Pageable pageable) {
        return springDataRepository.findAllByDeletedAtIsNull(pageable).map(CreditApplicationMapper::toDomain);
    }

    @Override
    public boolean existsByDocumentId(String documentId) {
        return springDataRepository.existsByDocumentId(documentId);
    }
}
