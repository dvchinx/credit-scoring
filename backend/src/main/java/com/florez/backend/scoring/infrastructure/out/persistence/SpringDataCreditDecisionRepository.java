package com.florez.backend.scoring.infrastructure.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCreditDecisionRepository extends JpaRepository<CreditDecisionJpaEntity, UUID> {

    List<CreditDecisionJpaEntity> findAllByCreditApplicationIdOrderByDecidedAtDesc(UUID creditApplicationId);
}
