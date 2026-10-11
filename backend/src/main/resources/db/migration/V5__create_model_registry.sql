-- Model registry: qué versiones de modelo existen (con su metadata de entrenamiento) y cuál
-- estaba activa en cada momento. Ambas tablas son append-only: una versión registrada nunca
-- cambia y una activación nunca se borra; un rollback es una activación nueva.
CREATE TABLE model_versions (
    version         VARCHAR(100)     PRIMARY KEY,
    algorithm       VARCHAR(500)     NOT NULL,
    hyperparameters JSONB            NOT NULL,
    trained_at      TIMESTAMPTZ      NOT NULL,
    dataset_source  VARCHAR(500)     NOT NULL,
    dataset_sha256  VARCHAR(64)      NOT NULL CHECK (dataset_sha256 ~ '^[0-9a-f]{64}$'),
    training_rows   INTEGER          NOT NULL CHECK (training_rows > 0),
    test_rows       INTEGER          NOT NULL CHECK (test_rows > 0),
    default_rate    DOUBLE PRECISION NOT NULL,
    metrics         JSONB            NOT NULL,
    feature_names   JSONB            NOT NULL,
    artifact_sha256 VARCHAR(64)      NOT NULL CHECK (artifact_sha256 ~ '^[0-9a-f]{64}$'),
    registered_by   VARCHAR(100)     NOT NULL,
    registered_at   TIMESTAMPTZ      NOT NULL
);

CREATE TABLE model_activations (
    id            BIGSERIAL    PRIMARY KEY,
    model_version VARCHAR(100) NOT NULL REFERENCES model_versions (version),
    reason        TEXT         NOT NULL CHECK (length(btrim(reason)) > 0),
    activated_by  VARCHAR(100) NOT NULL,
    activated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_model_activations_version ON model_activations (model_version);

CREATE FUNCTION forbid_append_only_changes() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '% es append-only: % no está permitido', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_model_versions_append_only
    BEFORE UPDATE OR DELETE ON model_versions
    FOR EACH ROW EXECUTE FUNCTION forbid_append_only_changes();

CREATE TRIGGER trg_model_activations_append_only
    BEFORE UPDATE OR DELETE ON model_activations
    FOR EACH ROW EXECUTE FUNCTION forbid_append_only_changes();

-- Desde ahora, toda decisión con versión de modelo debe apuntar a una versión registrada.
-- NOT VALID: las decisiones previas al registry (fases 2-3) se conservan tal cual, sin
-- inventar versiones registradas para ellas; la restricción aplica a toda fila nueva.
ALTER TABLE credit_decisions
    ADD CONSTRAINT fk_credit_decisions_model_version
    FOREIGN KEY (model_version) REFERENCES model_versions (version) NOT VALID;
