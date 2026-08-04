package com.florez.backend.creditapplication.domain.port.out;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CreditApplicationRepositoryPort {

    CreditApplication save(CreditApplication application);

    Optional<CreditApplication> findByIdAndNotDeleted(UUID id);

    Page<CreditApplication> findAllNotDeleted(Pageable pageable);

    boolean existsByDocumentId(String documentId);
}
