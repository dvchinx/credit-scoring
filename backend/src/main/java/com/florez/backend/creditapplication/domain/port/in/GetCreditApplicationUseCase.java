package com.florez.backend.creditapplication.domain.port.in;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import java.util.UUID;

public interface GetCreditApplicationUseCase {

    CreditApplication getById(UUID id);
}
