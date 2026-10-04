"""Verifica que la forma cerrada de app.model.explain coincide con la librería `shap`."""
import numpy as np
import pandas as pd
import pytest

from app.model import explain, load_model
from app.schemas import ScoreRequest
from tests.test_api import SAMPLE_REQUEST
from training.train import split_train_test

shap = pytest.importorskip("shap")


def test_explain_matches_shap_linear_explainer_with_training_background(
    trained_artifacts_dir, synthetic_training_frame
):
    loaded = load_model(trained_artifacts_dir)
    preprocessor, classifier = loaded.pipeline[:-1], loaded.pipeline[-1]
    X_train, _, _, _ = split_train_test(synthetic_training_frame)
    background = preprocessor.transform(X_train)
    request = ScoreRequest(**SAMPLE_REQUEST)

    explainer = shap.LinearExplainer(
        classifier, shap.maskers.Independent(background, max_samples=len(background))
    )
    standardized = preprocessor.transform(pd.DataFrame([request.model_dump()])[X_train.columns])
    expected = dict(zip(X_train.columns, explainer.shap_values(standardized)[0]))

    explanation = explain(loaded, request)

    assert explanation.base_value == pytest.approx(float(np.ravel(explainer.expected_value)[0]), abs=1e-9)
    for contribution in explanation.contributions:
        assert contribution.shap_value == pytest.approx(expected[contribution.feature], abs=1e-9)
