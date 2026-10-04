"""Servicio FastAPI que expone el modelo de riesgo crediticio."""
from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException

from app.config import get_settings
from app.model import LoadedModel, ModelNotFoundError, explain, load_model, score
from app.schemas import ExplainResponse, FeatureContributionResponse, ScoreRequest, ScoreResponse


def _direction(shap_value: float) -> str:
    if shap_value > 0:
        return "increases_risk"
    if shap_value < 0:
        return "decreases_risk"
    return "neutral"


def create_app(artifacts_dir: Path | None = None) -> FastAPI:
    resolved_artifacts_dir = artifacts_dir or get_settings().artifacts_dir
    state: dict[str, LoadedModel | None] = {"model": None}

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        try:
            state["model"] = load_model(resolved_artifacts_dir)
        except ModelNotFoundError:
            state["model"] = None
        yield
        state.clear()

    fastapi_app = FastAPI(title="Credit Scoring ML Service", version="0.2.0", lifespan=lifespan)

    def require_model() -> LoadedModel:
        loaded = state.get("model")
        if loaded is None:
            raise HTTPException(status_code=503, detail="El modelo de scoring no está disponible todavía.")
        return loaded

    @fastapi_app.get("/health")
    def health() -> dict:
        loaded = state.get("model")
        return {
            "status": "ok" if loaded else "degraded",
            "model_loaded": loaded is not None,
            "model_version": loaded.version if loaded else None,
        }

    @fastapi_app.post("/score", response_model=ScoreResponse)
    def score_application(request: ScoreRequest) -> ScoreResponse:
        loaded = require_model()
        probability_of_default, risk_score = score(loaded, request)
        return ScoreResponse(
            probability_of_default=probability_of_default,
            risk_score=risk_score,
            model_version=loaded.version,
        )

    @fastapi_app.post("/explain", response_model=ExplainResponse)
    def explain_application(request: ScoreRequest) -> ExplainResponse:
        loaded = require_model()
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
