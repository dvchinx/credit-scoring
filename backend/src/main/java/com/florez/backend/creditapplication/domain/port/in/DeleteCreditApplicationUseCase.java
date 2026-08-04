package com.florez.backend.creditapplication.domain.port.in;

import java.util.UUID;

public interface DeleteCreditApplicationUseCase {

    void delete(UUID id);
}
