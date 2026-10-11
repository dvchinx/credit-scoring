package com.florez.backend.modelregistry.domain.port.in;

import com.florez.backend.modelregistry.domain.model.ModelActivation;
import java.util.List;
import java.util.Optional;

public interface GetModelActivationsUseCase {

    /** Activación vigente: define la versión con la que se evalúan las solicitudes ahora. */
    Optional<ModelActivation> current();

    /** Historial de activaciones, de la más reciente a la más antigua. */
    List<ModelActivation> history();
}
