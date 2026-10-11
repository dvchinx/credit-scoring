package com.florez.backend.whatif.domain.port.in;

import com.florez.backend.whatif.domain.model.ApplicationChanges;
import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import java.util.UUID;

public interface SimulateWhatIfUseCase {

    /** Evalúa la solicitud tal como está y con los cambios, con el modelo activo. No crea decisiones. */
    WhatIfSimulation simulate(UUID creditApplicationId, ApplicationChanges changes, String requestedBy);
}
