package com.florez.backend.scoring.domain.port.in;

import com.florez.backend.scoring.domain.model.CreditDecision;
import java.util.UUID;

public interface EvaluateCreditApplicationUseCase {

    CreditDecision evaluate(UUID creditApplicationId, String requestedBy);
}
