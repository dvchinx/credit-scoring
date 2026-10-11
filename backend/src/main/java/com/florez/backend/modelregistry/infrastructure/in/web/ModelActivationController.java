package com.florez.backend.modelregistry.infrastructure.in.web;

import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.modelregistry.domain.port.in.ActivateModelVersionUseCase;
import com.florez.backend.modelregistry.domain.port.in.GetModelActivationsUseCase;
import com.florez.backend.modelregistry.infrastructure.in.web.dto.ActivateModelVersionRequest;
import com.florez.backend.modelregistry.infrastructure.in.web.dto.ModelActivationResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/model-activations")
public class ModelActivationController {

    private final ActivateModelVersionUseCase activateUseCase;
    private final GetModelActivationsUseCase getActivationsUseCase;

    public ModelActivationController(
            ActivateModelVersionUseCase activateUseCase, GetModelActivationsUseCase getActivationsUseCase) {
        this.activateUseCase = activateUseCase;
        this.getActivationsUseCase = getActivationsUseCase;
    }

    /** Activa una versión registrada. Para hacer rollback, se activa de nuevo una versión anterior. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModelActivationResponse> activate(
            @Valid @RequestBody ActivateModelVersionRequest request, Principal principal) {
        var activation = activateUseCase.activate(request.version(), request.reason(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ModelActivationResponse.from(activation));
    }

    @GetMapping
    public ResponseEntity<List<ModelActivationResponse>> history() {
        return ResponseEntity.ok(getActivationsUseCase.history().stream().map(ModelActivationResponse::from).toList());
    }

    @GetMapping("/current")
    public ResponseEntity<ModelActivationResponse> current() {
        return getActivationsUseCase.current()
                .map(activation -> ResponseEntity.ok(ModelActivationResponse.from(activation)))
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna versión de modelo activa"));
    }
}
