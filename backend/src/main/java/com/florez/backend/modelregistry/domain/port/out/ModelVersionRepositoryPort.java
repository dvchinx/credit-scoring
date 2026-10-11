package com.florez.backend.modelregistry.domain.port.out;

import com.florez.backend.modelregistry.domain.model.ModelVersion;
import java.util.List;
import java.util.Optional;

/** Almacén append-only de versiones registradas. */
public interface ModelVersionRepositoryPort {

    ModelVersion save(ModelVersion modelVersion);

    Optional<ModelVersion> findByVersion(String version);

    List<ModelVersion> findAllNewestFirst();

    boolean exists(String version);
}
