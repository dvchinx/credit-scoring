import numpy as np
import pandas as pd

from training.preprocessing import FEATURE_NAMES, TARGET_COLUMN, raw_dataframe_to_features
from training.train import train_and_save


def _synthetic_training_frame(rows: int = 150) -> pd.DataFrame:
    rng = np.random.default_rng(seed=7)
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


def test_raw_dataframe_to_features_renames_and_orders_columns():
    df = _synthetic_training_frame(rows=5)

    features = raw_dataframe_to_features(df)

    assert list(features.columns) == FEATURE_NAMES


def test_train_and_save_persists_versioned_artifacts(tmp_path):
    df = _synthetic_training_frame()

    metadata = train_and_save(df, tmp_path)

    version_dir = tmp_path / metadata["version"]
    assert (version_dir / "model.joblib").exists()
    assert (version_dir / "metadata.json").exists()
    assert (tmp_path / "ACTIVE_VERSION").read_text().strip() == metadata["version"]
    assert 0.0 <= metadata["metrics"]["roc_auc"] <= 1.0
