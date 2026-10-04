package com.florez.backend.creditapplication.infrastructure.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditApplicationRequest(
        @NotBlank(message = "el nombre del solicitante es obligatorio") String applicantFullName,
        @NotBlank(message = "el documento de identidad es obligatorio") String documentId,
        @NotNull(message = "la fecha de nacimiento es obligatoria")
        @Past(message = "la fecha de nacimiento debe ser en el pasado") LocalDate birthDate,
        @NotNull(message = "el ingreso mensual es obligatorio")
        @Positive(message = "el ingreso mensual debe ser positivo") BigDecimal monthlyIncome,
        @NotNull(message = "el monto solicitado es obligatorio")
        @Positive(message = "el monto solicitado debe ser positivo") BigDecimal requestedAmount,
        @NotNull(message = "el plazo en meses es obligatorio")
        @Positive(message = "el plazo en meses debe ser positivo") Integer loanTermMonths,
        @NotNull(message = "los años de antigüedad laboral son obligatorios")
        @PositiveOrZero(message = "los años de antigüedad laboral no pueden ser negativos") Double employmentYears,
        @NotNull(message = "la deuda mensual existente es obligatoria")
        @PositiveOrZero(message = "la deuda mensual existente no puede ser negativa") BigDecimal existingMonthlyDebt,
        @NotNull(message = "el número de dependientes es obligatorio")
        @PositiveOrZero(message = "el número de dependientes no puede ser negativo") Integer numberOfDependents,
        @NotNull(message = "el historial crediticio es obligatorio") @Valid CreditHistoryPayload creditHistory) {
}
