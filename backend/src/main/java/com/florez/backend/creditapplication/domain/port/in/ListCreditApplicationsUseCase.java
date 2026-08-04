package com.florez.backend.creditapplication.domain.port.in;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListCreditApplicationsUseCase {

    Page<CreditApplication> list(Pageable pageable);
}
