package com.florez.backend.whatif.infrastructure.in.web;

import com.florez.backend.whatif.domain.port.in.ListWhatIfSimulationsUseCase;
import com.florez.backend.whatif.domain.port.in.SimulateWhatIfUseCase;
import com.florez.backend.whatif.infrastructure.in.web.dto.WhatIfRequest;
import com.florez.backend.whatif.infrastructure.in.web.dto.WhatIfResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST (no GET) porque la simulación lleva un cuerpo con los cambios y queda registrada para
 * auditoría. Nunca crea una decisión ni cambia el estado de la solicitud.
 */
@RestController
@RequestMapping("/credit-applications/{applicationId}/what-if")
public class WhatIfController {

    private final SimulateWhatIfUseCase simulateUseCase;
    private final ListWhatIfSimulationsUseCase listUseCase;

    public WhatIfController(SimulateWhatIfUseCase simulateUseCase, ListWhatIfSimulationsUseCase listUseCase) {
        this.simulateUseCase = simulateUseCase;
        this.listUseCase = listUseCase;
    }

    @PostMapping
    public ResponseEntity<WhatIfResponse> simulate(
            @PathVariable UUID applicationId, @Valid @RequestBody WhatIfRequest request, Principal principal) {
        var simulation = simulateUseCase.simulate(applicationId, request.toDomain(), principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(WhatIfResponse.from(simulation));
    }

    @GetMapping
    public ResponseEntity<List<WhatIfResponse>> list(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(listUseCase.listByApplication(applicationId).stream().map(WhatIfResponse::from).toList());
    }
}
