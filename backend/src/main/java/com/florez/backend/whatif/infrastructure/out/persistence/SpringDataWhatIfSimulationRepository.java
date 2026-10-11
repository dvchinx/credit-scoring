package com.florez.backend.whatif.infrastructure.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataWhatIfSimulationRepository extends JpaRepository<WhatIfSimulationJpaEntity, UUID> {

    List<WhatIfSimulationJpaEntity> findAllByCreditApplicationIdOrderBySimulatedAtDesc(UUID creditApplicationId);
}
