package com.florez.backend.modelregistry.domain.port.in;

import com.florez.backend.modelregistry.domain.model.ModelVersion;
import java.util.List;

public interface GetModelVersionsUseCase {

    /** Versiones registradas, de la más reciente a la más antigua. */
    List<ModelVersion> list();

    ModelVersion get(String version);
}
