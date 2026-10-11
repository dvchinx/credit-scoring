package com.florez.backend.modelregistry.infrastructure.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataModelVersionRepository extends JpaRepository<ModelVersionJpaEntity, String> {

    List<ModelVersionJpaEntity> findAllByOrderByRegisteredAtDesc();
}
