-- Variables de historial crediticio (estilo buró) que necesita el modelo de riesgo.
-- El proyecto aún no tiene datos productivos: las filas existentes se rellenan con 0
-- ("sin historial reportado") y luego se elimina el DEFAULT para que toda solicitud
-- nueva tenga que informarlas explícitamente.
ALTER TABLE credit_applications
    ADD COLUMN revolving_utilization         DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN open_credit_lines             INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN real_estate_loans             INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN late_payments_30_59_days      INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN late_payments_60_89_days      INTEGER          NOT NULL DEFAULT 0,
    ADD COLUMN late_payments_90_days_or_more INTEGER          NOT NULL DEFAULT 0;

ALTER TABLE credit_applications
    ALTER COLUMN revolving_utilization DROP DEFAULT,
    ALTER COLUMN open_credit_lines DROP DEFAULT,
    ALTER COLUMN real_estate_loans DROP DEFAULT,
    ALTER COLUMN late_payments_30_59_days DROP DEFAULT,
    ALTER COLUMN late_payments_60_89_days DROP DEFAULT,
    ALTER COLUMN late_payments_90_days_or_more DROP DEFAULT;
