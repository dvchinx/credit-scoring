"""Servicio FastAPI que expone los modelos de riesgo crediticio entrenados."""
from __future__ import annotations

from pathlib import Path
from typing import Annotated

from fastapi import FastAPI, HTTPException, Query

from app.config import get_settings
from app.model import explain, score
from app.registry import VERSION_PATTERN, LoadedModel, ModelIntegrityError, ModelNotFoundError, ModelRegistry
from app.schemas import (
    ExplainResponse,
    FeatureContributionResponse,
    ModelVersionInfo,
    ScoreRequest,
    ScoreResponse,
)

ModelVersionParam = Annotated[
    str,
    Query(pattern=VERSION_PATTERN, description="Versión exacta del modelo a usar (la elige el backend)"),
]


def _direction(shap_value: float) -> str:
    if shap_value > 0:
        return "increases_risk"
    if shap_value < 0:
        return "decreases_risk"
    return "neutral"


def create_app(artifacts_dir: Path | None = None) -> FastAPI:
    registry = ModelRegistry(artifacts_dir or get_settings().artifacts_dir)
    fastapi_app = FastAPI(title="Credit Scoring ML Service", version="0.3.0")

    def load(version: str) -> LoadedModel:
        try:
            return registry.load(version)
        except ModelNotFoundError as error:
            raise HTTPException(status_code=404, detail=str(error)) from error
        except ModelIntegrityError as error:
            raise HTTPException(status_code=409, detail=str(error)) from error

    def describe(version: str) -> ModelVersionInfo:
        try:
            metadata = registry.describe(version)
        except ModelNotFoundError as error:
            raise HTTPException(status_code=404, detail=str(error)) from error
        return ModelVersionInfo(
            version=version,
            trained_at=metadata.get("trained_at"),
            algorithm=metadata.get("algorithm"),
            hyperparameters=metadata.get("hyperparameters") or {},
            dataset=metadata.get("dataset"),
            metrics=metadata.get("metrics") or {},
            feature_names=metadata.get("feature_names") or [],
            artifact_sha256=(metadata.get("artifact") or {}).get("sha256"),
            artifact_verified=metadata["artifact_verified"],
        )

    @fastapi_app.get("/health")
    def health() -> dict:
        versions = registry.list_versions()
        return {"status": "ok" if versions else "degraded", "available_versions": len(versions)}

    @fastapi_app.get("/models", response_model=list[ModelVersionInfo])
    def list_models() -> list[ModelVersionInfo]:
        return [describe(version) for version in registry.list_versions()]

    @fastapi_app.get("/models/{version}", response_model=ModelVersionInfo)
    def get_model(version: str) -> ModelVersionInfo:
        return describe(version)

    @fastapi_app.post("/score", response_model=ScoreResponse)
    def score_application(request: ScoreRequest, model_version: ModelVersionParam) -> ScoreResponse:
        loaded = load(model_version)
        probability_of_default, risk_score = score(loaded, request)
        return ScoreResponse(
            probability_of_default=probability_of_default,
            risk_score=risk_score,
            model_version=loaded.version,
        )

    @fastapi_app.post("/explain", response_model=ExplainResponse)
    def explain_application(request: ScoreRequest, model_version: ModelVersionParam) -> ExplainResponse:
        loaded = load(model_version)
        explanation = explain(loaded, request)
        return ExplainResponse(
            model_version=loaded.version,
            base_value=explanation.base_value,
            output_value=explanation.output_value,
            contributions=[
                FeatureContributionResponse(
                    feature=contribution.feature,
                    value=contribution.value,
                    shap_value=contribution.shap_value,
                    direction=_direction(contribution.shap_value),
                )
                for contribution in explanation.contributions
            ],
        )

    return fastapi_app


app = create_app()
