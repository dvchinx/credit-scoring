package com.florez.backend.modelregistry.domain.port.in;

import com.florez.backend.modelregistry.domain.model.ModelActivation;

public interface ActivateModelVersionUseCase {

    ModelActivation activate(String version, String reason, String activatedBy);
}
