package com.florez.backend.scoring.domain.model;

/**
 * {@code COMPLETED}: decisión final, siempre con score y explicación.
 * {@code FAILED}: intento de evaluación que no pudo completarse (p. ej. el servicio de ML no
 * respondió). Se registra para auditoría, pero no cambia el estado de la solicitud.
 */
public enum DecisionStatus {
    COMPLETED,
    FAILED
}
