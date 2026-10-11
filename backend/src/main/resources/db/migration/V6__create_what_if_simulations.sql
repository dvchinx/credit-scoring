-- Registro de simulaciones "qué pasaría si". Una simulación nunca es una decisión: no cambia el
-- estado de la solicitud. Se conserva para auditoría, por ejemplo para detectar si alguien probó
-- variantes hasta encontrar una que aprueba y luego editó la solicitud con esos valores.
CREATE TABLE what_if_simulations (
    id                    UUID         PRIMARY KEY,
    credit_application_id UUID         NOT NULL REFERENCES credit_applications (id),
    model_version         VARCHAR(100) NOT NULL REFERENCES model_versions (version),
    changes               JSONB        NOT NULL,
    baseline              JSONB        NOT NULL,
    scenario              JSONB        NOT NULL,
    baseline_outcome      VARCHAR(20)  NOT NULL,
    scenario_outcome      VARCHAR(20)  NOT NULL,
    policy                JSONB        NOT NULL,
    requested_by          VARCHAR(100) NOT NULL,
    simulated_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_what_if_simulations_application ON what_if_simulations (credit_application_id, simulated_at DESC);
-- Consulta típica de auditoría: simulaciones que cambiaron el resultado.
CREATE INDEX idx_what_if_simulations_outcome_change ON what_if_simulations (credit_application_id)
    WHERE baseline_outcome <> scenario_outcome;

CREATE TRIGGER trg_what_if_simulations_append_only
    BEFORE UPDATE OR DELETE ON what_if_simulations
    FOR EACH ROW EXECUTE FUNCTION forbid_append_only_changes();
