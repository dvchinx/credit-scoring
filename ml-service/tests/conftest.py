"""Fixtures compartidas. Entrenan un modelo sintético en un directorio temporal
para que los tests no dependan de descargar el dataset real de Kaggle.
"""
from __future__ import annotations

import numpy as np
import pandas as pd
import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from training.preprocessing import TARGET_COLUMN
from training.train import train_and_save


def _synthetic_training_frame(rows: int = 200) -> pd.DataFrame:
    rng = np.random.default_rng(seed=42)
    data = {
        "RevolvingUtilizationOfUnsecuredLines": rng.uniform(0, 1, rows),
        "age": rng.integers(18, 80, rows),
        "NumberOfTime30-59DaysPastDueNotWorse": rng.integers(0, 3, rows),
        "DebtRatio": rng.uniform(0, 1, rows),
        "MonthlyIncome": rng.uniform(1000, 10000, rows),
        "NumberOfOpenCreditLinesAndLoans": rng.integers(0, 15, rows),
        "NumberOfTimes90DaysLate": rng.integers(0, 3, rows),
        "NumberRealEstateLoansOrLines": rng.integers(0, 4, rows),
        "NumberOfTime60-89DaysPastDueNotWorse": rng.integers(0, 3, rows),
        "NumberOfDependents": rng.integers(0, 5, rows),
    }
    df = pd.DataFrame(data)
    df[TARGET_COLUMN] = rng.integers(0, 2, rows)
    return df


@pytest.fixture
def synthetic_training_frame() -> pd.DataFrame:
    return _synthetic_training_frame()


@pytest.fixture
def trained_artifacts_dir(tmp_path, synthetic_training_frame):
    train_and_save(synthetic_training_frame, tmp_path)
    return tmp_path


@pytest.fixture
def client(trained_artifacts_dir):
    fastapi_app = create_app(artifacts_dir=trained_artifacts_dir)
    with TestClient(fastapi_app) as test_client:
        yield test_client


@pytest.fixture
def empty_artifacts_client(tmp_path):
    fastapi_app = create_app(artifacts_dir=tmp_path)
    with TestClient(fastapi_app) as test_client:
        yield test_client
