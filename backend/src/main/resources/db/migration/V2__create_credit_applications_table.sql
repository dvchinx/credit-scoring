CREATE TABLE credit_applications (
    id                    UUID PRIMARY KEY,
    applicant_full_name   VARCHAR(200)   NOT NULL,
    document_id           VARCHAR(50)    NOT NULL UNIQUE,
    birth_date            DATE           NOT NULL,
    monthly_income        NUMERIC(14, 2) NOT NULL,
    requested_amount      NUMERIC(14, 2) NOT NULL,
    loan_term_months      INTEGER        NOT NULL,
    employment_years      DOUBLE PRECISION NOT NULL,
    existing_monthly_debt NUMERIC(14, 2) NOT NULL,
    number_of_dependents  INTEGER        NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    created_by            VARCHAR(100)   NOT NULL,
    created_at            TIMESTAMPTZ    NOT NULL,
    updated_at            TIMESTAMPTZ    NOT NULL,
    deleted_at            TIMESTAMPTZ
);

CREATE INDEX idx_credit_applications_deleted_at ON credit_applications (deleted_at);
