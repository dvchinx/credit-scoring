from typing import Literal

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


class FeatureContributionResponse(BaseModel):
    feature: str
    value: float = Field(..., description="Valor de entrada de la feature, tal como llegó en la solicitud")
    shap_value: float = Field(..., description="Contribución al log-odds de default (positivo = más riesgo)")
    direction: Literal["increases_risk", "decreases_risk", "neutral"]


class ExplainResponse(BaseModel):
    model_config = ConfigDict(protected_namespaces=())

    model_version: str
    base_value: float = Field(..., description="Log-odds de default esperado sobre los datos de entrenamiento")
    output_value: float = Field(..., description="Log-odds de default de esta solicitud: base_value + Σ shap_value")
    contributions: list[FeatureContributionResponse] = Field(
        ..., description="Contribución de cada feature, ordenadas por magnitud descendente"
    )


class DatasetInfo(BaseModel):
    source: str | None = None
    sha256: str | None = None
    training_rows: int | None = None
    test_rows: int | None = None
    default_rate: float | None = None


class ModelVersionInfo(BaseModel):
    """Metadata de una versión entrenada. Los campos opcionales pueden faltar en versiones
    entrenadas antes de la Fase 4; el backend se niega a registrar versiones incompletas."""

    version: str
    trained_at: str | None = None
    algorithm: str | None = None
    hyperparameters: dict[str, str | int | float | bool | None] = Field(default_factory=dict)
    dataset: DatasetInfo | None = None
    metrics: dict[str, float] = Field(default_factory=dict)
    feature_names: list[str] = Field(default_factory=list)
    artifact_sha256: str | None = None
    artifact_verified: bool = Field(
        ..., description="El artefacto en disco coincide con el hash registrado al entrenar"
    )


class WhatIfRequest(BaseModel):
    baseline: ScoreRequest = Field(..., description="Variables tal como están en la solicitud")
    scenario: ScoreRequest = Field(..., description="Variables con los cambios simulados")


class EvaluationResponse(BaseModel):
    probability_of_default: float
    risk_score: int
    base_value: float
    output_value: float
    contributions: list[FeatureContributionResponse]


class WhatIfResponse(BaseModel):
    """Ambos escenarios evaluados con el mismo modelo cargado: las diferencias se deben solo a la entrada."""

    model_config = ConfigDict(protected_namespaces=())

    model_version: str
    baseline: EvaluationResponse
    scenario: EvaluationResponse
