package com.florez.backend.whatif.domain.port.out;

import com.florez.backend.whatif.domain.model.WhatIfSimulation;
import java.util.List;
import java.util.UUID;

/** Registro append-only de simulaciones. */
public interface WhatIfSimulationRepositoryPort {

    WhatIfSimulation save(WhatIfSimulation simulation);

    List<WhatIfSimulation> findByApplicationIdNewestFirst(UUID creditApplicationId);
}
