package com.florez.backend.scoring.domain.port.in;

import com.florez.backend.scoring.domain.model.CreditDecision;
import java.util.List;
import java.util.UUID;

public interface ListCreditDecisionsUseCase {

    /** Historial completo de evaluaciones de la solicitud, de la más reciente a la más antigua. */
    List<CreditDecision> listByApplication(UUID creditApplicationId);
}
