package com.florez.backend.whatif.infrastructure.out.persistence;

import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import com.florez.backend.whatif.domain.port.out.WhatIfSimulationRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WhatIfSimulationRepositoryAdapter implements WhatIfSimulationRepositoryPort {

    private final SpringDataWhatIfSimulationRepository springDataRepository;

    public WhatIfSimulationRepositoryAdapter(SpringDataWhatIfSimulationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public WhatIfSimulation save(WhatIfSimulation simulation) {
        if (simulation.getId() != null) {
            throw new IllegalArgumentException("Las simulaciones son inmutables: no se puede volver a guardar " + simulation.getId());
        }
        return WhatIfSimulationMapper.toDomain(springDataRepository.save(WhatIfSimulationMapper.toNewEntity(simulation)));
    }

    @Override
    public List<WhatIfSimulation> findByApplicationIdNewestFirst(UUID creditApplicationId) {
        return springDataRepository.findAllByCreditApplicationIdOrderBySimulatedAtDesc(creditApplicationId).stream()
                .map(WhatIfSimulationMapper::toDomain)
                .toList();
    }
}
