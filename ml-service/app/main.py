"""Servicio FastAPI que expone el modelo de riesgo crediticio."""
from __future__ import annotations

from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException

from app.config import get_settings
from app.model import LoadedModel, ModelNotFoundError, load_model, score
from app.schemas import ScoreRequest, ScoreResponse


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

    fastapi_app = FastAPI(title="Credit Scoring ML Service", version="0.1.0", lifespan=lifespan)

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
        loaded = state.get("model")
        if loaded is None:
            raise HTTPException(status_code=503, detail="El modelo de scoring no está disponible todavía.")

        probability_of_default, risk_score = score(loaded, request)
        return ScoreResponse(
            probability_of_default=probability_of_default,
            risk_score=risk_score,
            model_version=loaded.version,
        )

    return fastapi_app


app = create_app()
