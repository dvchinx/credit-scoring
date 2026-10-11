package com.florez.backend.modelregistry.domain.port.in;

import com.florez.backend.modelregistry.domain.model.ModelVersion;

public interface RegisterModelVersionUseCase {

    /** Registra una versión que ya existe en el servicio de ML, tomando su metadata de allí. */
    ModelVersion register(String version, String registeredBy);
}
