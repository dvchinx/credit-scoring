import numpy as np
import pandas as pd
import pytest

from app.registry import sha256_of_file
from training import train

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


def test_train_and_save_persists_versioned_artifacts_with_complete_metadata(tmp_path):
    df = _synthetic_training_frame()

    metadata = train_and_save(df, tmp_path, dataset_source="cs-training.csv")

    version_dir = tmp_path / metadata["version"]
    assert (version_dir / "model.joblib").exists()
    assert (version_dir / "metadata.json").exists()
    assert metadata["artifact"]["sha256"] == sha256_of_file(version_dir / "model.joblib")
    assert metadata["dataset"]["source"] == "cs-training.csv"
    assert metadata["dataset"]["training_rows"] + metadata["dataset"]["test_rows"] == len(df)
    assert 0.0 <= metadata["metrics"]["roc_auc"] <= 1.0
    assert 0.0 <= metadata["metrics"]["brier_score"] <= 1.0
    assert metadata["feature_names"] == FEATURE_NAMES


def test_training_does_not_activate_anything(tmp_path):
    train_and_save(_synthetic_training_frame(), tmp_path)

    assert not (tmp_path / "ACTIVE_VERSION").exists()


def test_training_never_overwrites_an_existing_version(tmp_path, monkeypatch):
    monkeypatch.setattr(train, "new_version_id", lambda: "20260101T000000Z")
    train_and_save(_synthetic_training_frame(), tmp_path)
    original_hash = sha256_of_file(tmp_path / "20260101T000000Z" / "model.joblib")

    with pytest.raises(FileExistsError):
        train_and_save(_synthetic_training_frame(), tmp_path)

    assert sha256_of_file(tmp_path / "20260101T000000Z" / "model.joblib") == original_hash


def test_dataset_fingerprint_changes_with_content():
    df = _synthetic_training_frame(rows=20)
    changed = df.copy()
    changed.loc[changed.index[0], "age"] += 1

    assert train.dataset_fingerprint(df) == train.dataset_fingerprint(df.copy())
    assert train.dataset_fingerprint(df) != train.dataset_fingerprint(changed)
