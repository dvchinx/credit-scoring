from pydantic import BaseModel, ConfigDict, Field


class ScoreRequest(BaseModel):
    revolving_utilization_of_unsecured_lines: float = Field(
        ..., ge=0, description="Saldo de líneas de crédito no garantizadas dividido por el límite total"
    )
    age: int = Field(..., gt=0)
    number_of_time_30_59_days_past_due_not_worse: int = Field(..., ge=0)
    debt_ratio: float = Field(..., ge=0)
    monthly_income: float = Field(..., ge=0)
    number_of_open_credit_lines_and_loans: int = Field(..., ge=0)
    number_of_times_90_days_late: int = Field(..., ge=0)
    number_real_estate_loans_or_lines: int = Field(..., ge=0)
    number_of_time_60_89_days_past_due_not_worse: int = Field(..., ge=0)
    number_of_dependents: float = Field(..., ge=0)


class ScoreResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    probability_of_default: float
    risk_score: int
    model_version: str
