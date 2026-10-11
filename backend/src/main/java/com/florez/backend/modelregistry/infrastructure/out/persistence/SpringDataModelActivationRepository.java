package com.florez.backend.modelregistry.infrastructure.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataModelActivationRepository extends JpaRepository<ModelActivationJpaEntity, Long> {

    Optional<ModelActivationJpaEntity> findFirstByOrderByIdDesc();

    List<ModelActivationJpaEntity> findAllByOrderByIdDesc();
}
