"""Carga el pipeline entrenado activo y calcula el score de riesgo."""
from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

import joblib
import pandas as pd

from app.schemas import ScoreRequest
from training.preprocessing import FEATURE_NAMES


@dataclass(frozen=True)
class LoadedModel:
    pipeline: object
    version: str
    metadata: dict


class ModelNotFoundError(RuntimeError):
    pass


def resolve_active_version_dir(artifacts_dir: Path) -> Path:
    active_version_file = artifacts_dir / "ACTIVE_VERSION"
    if not active_version_file.exists():
        raise ModelNotFoundError(
            f"No hay versión activa en '{artifacts_dir}'. Ejecuta 'python -m training.train' primero."
        )
    version = active_version_file.read_text().strip()
    version_dir = artifacts_dir / version
    if not version_dir.exists():
        raise ModelNotFoundError(f"La versión activa '{version}' no existe en '{artifacts_dir}'.")
    return version_dir


def load_model(artifacts_dir: Path) -> LoadedModel:
    version_dir = resolve_active_version_dir(artifacts_dir)
    pipeline = joblib.load(version_dir / "model.joblib")
    metadata = json.loads((version_dir / "metadata.json").read_text())
    return LoadedModel(pipeline=pipeline, version=metadata["version"], metadata=metadata)


def probability_to_risk_score(probability_of_default: float, min_score: int = 300, max_score: int = 850) -> int:
    """Mapea la probabilidad de default a una escala estilo FICO: a mayor score, menor riesgo."""
    return round(max_score - probability_of_default * (max_score - min_score))


def score(loaded_model: LoadedModel, request: ScoreRequest) -> tuple[float, int]:
    features = pd.DataFrame([request.model_dump()])[FEATURE_NAMES]
    probability_of_default = float(loaded_model.pipeline.predict_proba(features)[0, 1])
    risk_score = probability_to_risk_score(probability_of_default)
    return probability_of_default, risk_score
