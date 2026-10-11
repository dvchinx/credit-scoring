package com.florez.backend.whatif.domain.port.in;

import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import java.util.List;
import java.util.UUID;

public interface ListWhatIfSimulationsUseCase {

    /** Simulaciones hechas sobre la solicitud, de la más reciente a la más antigua. */
    List<WhatIfSimulation> listByApplication(UUID creditApplicationId);
}
