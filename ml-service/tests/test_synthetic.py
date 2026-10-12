import pandas as pd

from app.registry import ModelRegistry
from training.preprocessing import FEATURE_NAMES, TARGET_COLUMN, raw_dataframe_to_features
from training.synthetic import generate
from training.train import train_and_save


def test_generate_produces_the_kaggle_schema_with_missing_values_like_the_real_dataset():
    df = generate(rows=2000)

    assert list(raw_dataframe_to_features(df).columns) == FEATURE_NAMES
    assert set(df[TARGET_COLUMN].unique()) == {0, 1}
    assert df["MonthlyIncome"].isna().any()
    assert 0.03 < df[TARGET_COLUMN].mean() < 0.25


def test_generate_is_deterministic_for_a_seed():
    pd.testing.assert_frame_equal(generate(rows=500, seed=1), generate(rows=500, seed=1))


def test_a_model_trained_on_it_learns_sensible_risk_directions(tmp_path):
    metadata = train_and_save(generate(rows=8000), tmp_path, dataset_source="synthetic-demo.csv")
    classifier = ModelRegistry(tmp_path).load(metadata["version"]).pipeline[-1]
    coefficients = dict(zip(FEATURE_NAMES, classifier.coef_[0]))

    assert metadata["metrics"]["roc_auc"] > 0.70  # aprende señal; no es una métrica del mundo real
    assert coefficients["number_of_times_90_days_late"] > 0
    assert coefficients["revolving_utilization_of_unsecured_lines"] > 0
    assert coefficients["age"] < 0
