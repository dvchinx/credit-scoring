package com.florez.backend.modelregistry.infrastructure.in.web;

import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.port.in.GetModelActivationsUseCase;
import com.florez.backend.modelregistry.domain.port.in.GetModelVersionsUseCase;
import com.florez.backend.modelregistry.domain.port.in.RegisterModelVersionUseCase;
import com.florez.backend.modelregistry.infrastructure.in.web.dto.ModelVersionResponse;
import com.florez.backend.modelregistry.infrastructure.in.web.dto.RegisterModelVersionRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/model-versions")
public class ModelVersionController {

    private final RegisterModelVersionUseCase registerUseCase;
    private final GetModelVersionsUseCase getVersionsUseCase;
    private final GetModelActivationsUseCase getActivationsUseCase;

    public ModelVersionController(
            RegisterModelVersionUseCase registerUseCase,
            GetModelVersionsUseCase getVersionsUseCase,
            GetModelActivationsUseCase getActivationsUseCase) {
        this.registerUseCase = registerUseCase;
        this.getVersionsUseCase = getVersionsUseCase;
        this.getActivationsUseCase = getActivationsUseCase;
    }

    /** Registra una versión ya entrenada; la metadata se toma del servicio de ML, no del cliente. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModelVersionResponse> register(
            @Valid @RequestBody RegisterModelVersionRequest request, Principal principal) {
        var registered = registerUseCase.register(request.version(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ModelVersionResponse.from(registered, false));
    }

    @GetMapping
    public ResponseEntity<List<ModelVersionResponse>> list() {
        String active = activeVersion();
        return ResponseEntity.ok(getVersionsUseCase.list().stream()
                .map(version -> ModelVersionResponse.from(version, version.version().equals(active)))
                .toList());
    }

    @GetMapping("/{version}")
    public ResponseEntity<ModelVersionResponse> get(@PathVariable String version) {
        return ResponseEntity.ok(ModelVersionResponse.from(getVersionsUseCase.get(version), version.equals(activeVersion())));
    }

    private String activeVersion() {
        return getActivationsUseCase.current().map(ModelActivation::modelVersion).orElse(null);
    }
}
