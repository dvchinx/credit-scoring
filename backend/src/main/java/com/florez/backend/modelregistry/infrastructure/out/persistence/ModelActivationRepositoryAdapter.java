package com.florez.backend.modelregistry.infrastructure.out.persistence;

import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.port.out.ModelActivationRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ModelActivationRepositoryAdapter implements ModelActivationRepositoryPort {

    private final SpringDataModelActivationRepository springDataRepository;

    public ModelActivationRepositoryAdapter(SpringDataModelActivationRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ModelActivation save(ModelActivation activation) {
        if (activation.id() != null) {
            throw new IllegalArgumentException("Las activaciones son inmutables: no se puede volver a guardar " + activation.id());
        }
        return ModelRegistryMapper.toDomain(springDataRepository.save(ModelRegistryMapper.toNewEntity(activation)));
    }

    @Override
    public Optional<ModelActivation> findCurrent() {
        return springDataRepository.findFirstByOrderByIdDesc().map(ModelRegistryMapper::toDomain);
    }

    @Override
    public List<ModelActivation> findAllNewestFirst() {
        return springDataRepository.findAllByOrderByIdDesc().stream().map(ModelRegistryMapper::toDomain).toList();
    }
}
