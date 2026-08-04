package com.florez.backend.creditapplication.infrastructure.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCreditApplicationRepository extends JpaRepository<CreditApplicationJpaEntity, UUID> {

    Optional<CreditApplicationJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<CreditApplicationJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);

    boolean existsByDocumentId(String documentId);
}
