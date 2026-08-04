package com.florez.backend.creditapplication.infrastructure.in.web;

import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.port.in.CreateCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.in.DeleteCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.in.GetCreditApplicationUseCase;
import com.florez.backend.creditapplication.domain.port.in.ListCreditApplicationsUseCase;
import com.florez.backend.creditapplication.domain.port.in.UpdateCreditApplicationUseCase;
import com.florez.backend.creditapplication.infrastructure.in.web.dto.CreditApplicationRequest;
import com.florez.backend.creditapplication.infrastructure.in.web.dto.CreditApplicationResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/credit-applications")
public class CreditApplicationController {

    private final CreateCreditApplicationUseCase createUseCase;
    private final UpdateCreditApplicationUseCase updateUseCase;
    private final GetCreditApplicationUseCase getUseCase;
    private final ListCreditApplicationsUseCase listUseCase;
    private final DeleteCreditApplicationUseCase deleteUseCase;

    public CreditApplicationController(
            CreateCreditApplicationUseCase createUseCase,
            UpdateCreditApplicationUseCase updateUseCase,
            GetCreditApplicationUseCase getUseCase,
            ListCreditApplicationsUseCase listUseCase,
            DeleteCreditApplicationUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    public ResponseEntity<CreditApplicationResponse> create(
            @Valid @RequestBody CreditApplicationRequest request, Principal principal) {
        CreditApplication toCreate = toDomain(request, principal.getName());
        CreditApplication created = createUseCase.create(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(CreditApplicationResponse.from(created));
    }

    @GetMapping
    public ResponseEntity<Page<CreditApplicationResponse>> list(Pageable pageable) {
        Page<CreditApplicationResponse> page = listUseCase.list(pageable).map(CreditApplicationResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditApplicationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(CreditApplicationResponse.from(getUseCase.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditApplicationResponse> update(
            @PathVariable UUID id, @Valid @RequestBody CreditApplicationRequest request, Principal principal) {
        CreditApplication toUpdate = toDomain(request, principal.getName());
        CreditApplication updated = updateUseCase.update(id, toUpdate);
        return ResponseEntity.ok(CreditApplicationResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    private CreditApplication toDomain(CreditApplicationRequest request, String createdBy) {
        return CreditApplication.createNew(
                request.applicantFullName(),
                request.documentId(),
                request.birthDate(),
                request.monthlyIncome(),
                request.requestedAmount(),
                request.loanTermMonths(),
                request.employmentYears(),
                request.existingMonthlyDebt(),
                request.numberOfDependents(),
                createdBy);
    }
}
