"""Carga el pipeline entrenado activo, calcula el score de riesgo y su explicación."""
from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from app.schemas import ScoreRequest
from training.preprocessing import FEATURE_NAMES


@dataclass(frozen=True)
class LoadedModel:
    pipeline: object
    version: str
    metadata: dict


@dataclass(frozen=True)
class FeatureContribution:
    feature: str
    value: float
    shap_value: float


@dataclass(frozen=True)
class Explanation:
    base_value: float
    output_value: float
    contributions: list[FeatureContribution]


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


def _to_features_frame(request: ScoreRequest) -> pd.DataFrame:
    return pd.DataFrame([request.model_dump()])[FEATURE_NAMES]


def score(loaded_model: LoadedModel, request: ScoreRequest) -> tuple[float, int]:
    features = _to_features_frame(request)
    probability_of_default = float(loaded_model.pipeline.predict_proba(features)[0, 1])
    risk_score = probability_to_risk_score(probability_of_default)
    return probability_of_default, risk_score


def explain(loaded_model: LoadedModel, request: ScoreRequest) -> Explanation:
    """Valores SHAP exactos de la solicitud, en espacio log-odds de probabilidad de default.

    Para un modelo lineal f(z) = b + Σ βᵢ·zᵢ con features independientes, el valor SHAP
    tiene forma cerrada: φᵢ = βᵢ·(zᵢ − E[zᵢ]). Como el StandardScaler se ajusta sobre el
    set de entrenamiento (ya imputado), E[zᵢ] = 0 y por tanto φᵢ = βᵢ·zᵢ, con valor base
    E[f(z)] = b (el intercepto). Es el mismo resultado que shap.LinearExplainer usando el
    set de entrenamiento como background (verificado en tests), sin cargar `shap` en runtime.

    Propiedad de exactitud (local accuracy): base_value + Σ φᵢ = log-odds de la predicción.
    Valores positivos aumentan el riesgo de default; negativos lo reducen.
    """
    features = _to_features_frame(request)
    pipeline = loaded_model.pipeline
    preprocessor, classifier = pipeline[:-1], pipeline[-1]

    standardized = preprocessor.transform(features)[0]
    shap_values = classifier.coef_[0] * standardized
    base_value = float(classifier.intercept_[0])

    contributions = [
        FeatureContribution(feature=name, value=float(raw_value), shap_value=float(shap_value))
        for name, raw_value, shap_value in zip(FEATURE_NAMES, features.iloc[0], shap_values)
    ]
    contributions.sort(key=lambda contribution: abs(contribution.shap_value), reverse=True)

    return Explanation(
        base_value=base_value,
        output_value=base_value + float(np.sum(shap_values)),
        contributions=contributions,
    )
