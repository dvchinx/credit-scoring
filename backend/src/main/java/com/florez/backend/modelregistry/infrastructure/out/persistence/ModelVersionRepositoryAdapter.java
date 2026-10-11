package com.florez.backend.modelregistry.infrastructure.out.persistence;

import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ModelVersionRepositoryAdapter implements ModelVersionRepositoryPort {

    private final SpringDataModelVersionRepository springDataRepository;

    public ModelVersionRepositoryAdapter(SpringDataModelVersionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ModelVersion save(ModelVersion modelVersion) {
        return ModelRegistryMapper.toDomain(springDataRepository.save(ModelRegistryMapper.toNewEntity(modelVersion)));
    }

    @Override
    public Optional<ModelVersion> findByVersion(String version) {
        return springDataRepository.findById(version).map(ModelRegistryMapper::toDomain);
    }

    @Override
    public List<ModelVersion> findAllNewestFirst() {
        return springDataRepository.findAllByOrderByRegisteredAtDesc().stream().map(ModelRegistryMapper::toDomain).toList();
    }

    @Override
    public boolean exists(String version) {
        return springDataRepository.existsById(version);
    }
}
