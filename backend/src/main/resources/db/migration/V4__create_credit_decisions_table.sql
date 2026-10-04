-- Registro inmutable de cada evaluación de una solicitud de crédito: qué datos vio el
-- modelo, qué versión de modelo respondió, qué explicó y qué política se aplicó. Cada
-- reevaluación inserta una fila nueva; nunca se modifica ni borra una existente.
CREATE TABLE credit_decisions (
    id                     UUID PRIMARY KEY,
    credit_application_id  UUID             NOT NULL REFERENCES credit_applications (id),
    status                 VARCHAR(20)      NOT NULL,
    outcome                VARCHAR(20),
    probability_of_default DOUBLE PRECISION,
    risk_score             INTEGER,
    model_version          VARCHAR(100),
    application_snapshot   JSONB            NOT NULL,
    model_input            JSONB            NOT NULL,
    explanation            JSONB,
    policy                 JSONB            NOT NULL,
    reasons                JSONB            NOT NULL,
    failure_reason         TEXT,
    decided_by             VARCHAR(100)     NOT NULL,
    decided_at             TIMESTAMPTZ      NOT NULL,

    CONSTRAINT chk_credit_decisions_status CHECK (status IN ('COMPLETED', 'FAILED')),
    -- Ninguna decisión final existe sin score, explicación y versión de modelo.
    CONSTRAINT chk_credit_decisions_completed_is_explained CHECK (
        status <> 'COMPLETED' OR (
            outcome IS NOT NULL
            AND probability_of_default IS NOT NULL
            AND risk_score IS NOT NULL
            AND model_version IS NOT NULL
            AND explanation IS NOT NULL
        )
    ),
    CONSTRAINT chk_credit_decisions_failed_has_reason CHECK (
        status <> 'FAILED' OR (outcome IS NULL AND failure_reason IS NOT NULL)
    )
);

CREATE INDEX idx_credit_decisions_application ON credit_decisions (credit_application_id, decided_at DESC);
CREATE INDEX idx_credit_decisions_model_version ON credit_decisions (model_version);

CREATE FUNCTION forbid_credit_decision_changes() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'credit_decisions es append-only: % no está permitido', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_credit_decisions_append_only
    BEFORE UPDATE OR DELETE ON credit_decisions
    FOR EACH ROW EXECUTE FUNCTION forbid_credit_decision_changes();
