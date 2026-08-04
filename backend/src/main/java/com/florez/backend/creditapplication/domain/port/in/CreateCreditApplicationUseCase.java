package com.florez.backend.creditapplication.domain.port.in;

import com.florez.backend.creditapplication.domain.model.CreditApplication;

public interface CreateCreditApplicationUseCase {

    CreditApplication create(CreditApplication newApplication);
}
