package com.florez.backend.scoring.domain.port.out;

import com.florez.backend.scoring.domain.model.CreditDecision;
import java.util.List;
import java.util.UUID;

/** Almacén append-only de decisiones: solo se insertan, nunca se modifican. */
public interface CreditDecisionRepositoryPort {

    CreditDecision save(CreditDecision decision);

    List<CreditDecision> findByApplicationIdNewestFirst(UUID creditApplicationId);
}
