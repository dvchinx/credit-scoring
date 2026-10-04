package com.florez.backend.scoring.infrastructure.out.persistence;

import com.florez.backend.scoring.domain.model.CreditDecision;
import com.florez.backend.scoring.domain.port.out.CreditDecisionRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CreditDecisionRepositoryAdapter implements CreditDecisionRepositoryPort {

    private final SpringDataCreditDecisionRepository springDataRepository;

    public CreditDecisionRepositoryAdapter(SpringDataCreditDecisionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CreditDecision save(CreditDecision decision) {
        if (decision.getId() != null) {
            throw new IllegalArgumentException("Las decisiones son inmutables: no se puede volver a guardar " + decision.getId());
        }
        CreditDecisionJpaEntity saved = springDataRepository.save(CreditDecisionMapper.toNewEntity(decision));
        return CreditDecisionMapper.toDomain(saved);
    }

    @Override
    public List<CreditDecision> findByApplicationIdNewestFirst(UUID creditApplicationId) {
        return springDataRepository.findAllByCreditApplicationIdOrderByDecidedAtDesc(creditApplicationId).stream()
                .map(CreditDecisionMapper::toDomain)
                .toList();
    }
}
