package com.florez.backend.modelregistry.infrastructure.out.persistence;

import static com.florez.backend.common.infrastructure.persistence.JsonColumnCodec.read;
import static com.florez.backend.common.infrastructure.persistence.JsonColumnCodec.write;

import com.fasterxml.jackson.core.type.TypeReference;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.model.ModelArtifact;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.model.TrainingDataset;
import java.util.List;
import java.util.Map;

final class ModelRegistryMapper {

    private ModelRegistryMapper() {
    }

    static ModelVersionJpaEntity toNewEntity(ModelVersion modelVersion) {
        ModelArtifact artifact = modelVersion.artifact();
        TrainingDataset dataset = artifact.dataset();
        return new ModelVersionJpaEntity(
                artifact.version(),
                artifact.algorithm(),
                write(artifact.hyperparameters()),
                artifact.trainedAt(),
                dataset.source(),
                dataset.sha256(),
                dataset.trainingRows(),
                dataset.testRows(),
                dataset.defaultRate(),
                write(artifact.metrics()),
                write(artifact.featureNames()),
                artifact.artifactSha256(),
                modelVersion.registeredBy(),
                modelVersion.registeredAt());
    }

    static ModelVersion toDomain(ModelVersionJpaEntity entity) {
        ModelArtifact artifact = new ModelArtifact(
                entity.getId(),
                entity.getAlgorithm(),
                read(entity.getHyperparameters(), new TypeReference<Map<String, Object>>() { }),
                entity.getTrainedAt(),
                new TrainingDataset(
                        entity.getDatasetSource(),
                        entity.getDatasetSha256(),
                        entity.getTrainingRows(),
                        entity.getTestRows(),
                        entity.getDefaultRate()),
                read(entity.getMetrics(), new TypeReference<Map<String, Double>>() { }),
                read(entity.getFeatureNames(), new TypeReference<List<String>>() { }),
                entity.getArtifactSha256());
        return new ModelVersion(artifact, entity.getRegisteredBy(), entity.getRegisteredAt());
    }

    static ModelActivationJpaEntity toNewEntity(ModelActivation activation) {
        return new ModelActivationJpaEntity(
                activation.modelVersion(), activation.reason(), activation.activatedBy(), activation.activatedAt());
    }

    static ModelActivation toDomain(ModelActivationJpaEntity entity) {
        return new ModelActivation(
                entity.getId(), entity.getModelVersion(), entity.getReason(), entity.getActivatedBy(), entity.getActivatedAt());
    }
}
