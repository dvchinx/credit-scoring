package com.florez.backend.modelregistry.domain.port.out;

import com.florez.backend.modelregistry.domain.model.ModelActivation;
import java.util.List;
import java.util.Optional;

/** Log append-only de activaciones. La activación vigente es la última insertada. */
public interface ModelActivationRepositoryPort {

    ModelActivation save(ModelActivation activation);

    Optional<ModelActivation> findCurrent();

    List<ModelActivation> findAllNewestFirst();
}
