package com.florez.backend.scoring.infrastructure.in.web;

import com.florez.backend.scoring.domain.port.in.EvaluateCreditApplicationUseCase;
import com.florez.backend.scoring.domain.port.in.ListCreditDecisionsUseCase;
import com.florez.backend.scoring.infrastructure.in.web.dto.CreditDecisionResponse;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/credit-applications/{applicationId}/decisions")
public class CreditDecisionController {

    private final EvaluateCreditApplicationUseCase evaluateUseCase;
    private final ListCreditDecisionsUseCase listUseCase;

    public CreditDecisionController(
            EvaluateCreditApplicationUseCase evaluateUseCase, ListCreditDecisionsUseCase listUseCase) {
        this.evaluateUseCase = evaluateUseCase;
        this.listUseCase = listUseCase;
    }

    /** Evalúa la solicitud con el modelo activo. Cada llamada crea una decisión nueva e inmutable. */
    @PostMapping
    public ResponseEntity<CreditDecisionResponse> evaluate(@PathVariable UUID applicationId, Principal principal) {
        CreditDecisionResponse body = CreditDecisionResponse.from(evaluateUseCase.evaluate(applicationId, principal.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<List<CreditDecisionResponse>> list(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(listUseCase.listByApplication(applicationId).stream()
                .map(CreditDecisionResponse::from)
                .toList());
    }
}
