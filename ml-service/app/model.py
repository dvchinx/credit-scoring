"""Calcula el score de riesgo de un modelo cargado y su explicación."""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np
import pandas as pd

from app.registry import LoadedModel
from app.schemas import ScoreRequest
from training.preprocessing import FEATURE_NAMES


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
