import math

import pytest

SAMPLE_REQUEST = {
    "revolving_utilization_of_unsecured_lines": 0.3,
    "age": 45,
    "number_of_time_30_59_days_past_due_not_worse": 0,
    "debt_ratio": 0.2,
    "monthly_income": 5000,
    "number_of_open_credit_lines_and_loans": 5,
    "number_of_times_90_days_late": 0,
    "number_real_estate_loans_or_lines": 1,
    "number_of_time_60_89_days_past_due_not_worse": 0,
    "number_of_dependents": 1,
}


def test_health_reports_available_versions(client):
    body = client.get("/health").json()

    assert body == {"status": "ok", "available_versions": 1}


def test_health_degraded_without_trained_models(empty_artifacts_client):
    body = empty_artifacts_client.get("/health").json()

    assert body == {"status": "degraded", "available_versions": 0}


def test_list_models_returns_complete_verified_metadata(client, trained_version):
    models = client.get("/models").json()

    assert [model["version"] for model in models] == [trained_version]
    model = models[0]
    assert model["artifact_verified"] is True
    assert len(model["artifact_sha256"]) == 64
    assert model["algorithm"] == "SimpleImputer -> StandardScaler -> LogisticRegression"
    assert model["hyperparameters"]["class_weight"] == "balanced"
    assert model["dataset"]["source"] == "synthetic"
    assert len(model["dataset"]["sha256"]) == 64
    assert set(model["metrics"]) == {"roc_auc", "brier_score"}
    assert model["feature_names"] == list(SAMPLE_REQUEST)


def test_get_model_unknown_version_returns_404(client):
    assert client.get("/models/20990101T000000Z").status_code == 404


def test_score_returns_probability_and_risk_score_for_requested_version(client, trained_version):
    response = client.post("/score", params={"model_version": trained_version}, json=SAMPLE_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["probability_of_default"] <= 1.0
    assert 300 <= body["risk_score"] <= 850
    assert body["model_version"] == trained_version


def test_score_requires_model_version(client):
    assert client.post("/score", json=SAMPLE_REQUEST).status_code == 422


def test_score_rejects_malformed_model_version(client):
    response = client.post("/score", params={"model_version": "../secrets"}, json=SAMPLE_REQUEST)

    assert response.status_code == 422


def test_score_unknown_version_returns_404(client):
    response = client.post("/score", params={"model_version": "20990101T000000Z"}, json=SAMPLE_REQUEST)

    assert response.status_code == 404


def test_score_rejects_incomplete_payload(client, trained_version):
    incomplete_request = dict(SAMPLE_REQUEST)
    del incomplete_request["age"]

    response = client.post("/score", params={"model_version": trained_version}, json=incomplete_request)

    assert response.status_code == 422


def test_score_rejects_negative_values(client, trained_version):
    invalid_request = dict(SAMPLE_REQUEST, debt_ratio=-1)

    response = client.post("/score", params={"model_version": trained_version}, json=invalid_request)

    assert response.status_code == 422


def test_score_refuses_tampered_artifact(client, trained_artifacts_dir, trained_version):
    with (trained_artifacts_dir / trained_version / "model.joblib").open("ab") as artifact:
        artifact.write(b"tampered")

    response = client.post("/score", params={"model_version": trained_version}, json=SAMPLE_REQUEST)

    assert response.status_code == 409
    assert client.get(f"/models/{trained_version}").json()["artifact_verified"] is False


def test_explain_returns_one_contribution_per_feature_sorted_by_magnitude(client, trained_version):
    response = client.post("/explain", params={"model_version": trained_version}, json=SAMPLE_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert body["model_version"] == trained_version
    contributions = body["contributions"]
    assert {c["feature"] for c in contributions} == set(SAMPLE_REQUEST)
    magnitudes = [abs(c["shap_value"]) for c in contributions]
    assert magnitudes == sorted(magnitudes, reverse=True)


def test_explain_contributions_add_up_to_the_scored_probability(client, trained_version):
    params = {"model_version": trained_version}
    explanation = client.post("/explain", params=params, json=SAMPLE_REQUEST).json()
    scored = client.post("/score", params=params, json=SAMPLE_REQUEST).json()

    total_shap = sum(c["shap_value"] for c in explanation["contributions"])
    assert explanation["output_value"] == pytest.approx(explanation["base_value"] + total_shap)
    assert 1 / (1 + math.exp(-explanation["output_value"])) == pytest.approx(scored["probability_of_default"])
    assert explanation["model_version"] == scored["model_version"]


def test_explain_direction_matches_shap_sign(client, trained_version):
    contributions = client.post(
        "/explain", params={"model_version": trained_version}, json=SAMPLE_REQUEST
    ).json()["contributions"]

    for contribution in contributions:
        expected = "increases_risk" if contribution["shap_value"] > 0 else "decreases_risk"
        assert contribution["direction"] == expected


def test_explain_unknown_version_returns_404(empty_artifacts_client):
    response = empty_artifacts_client.post(
        "/explain", params={"model_version": "20990101T000000Z"}, json=SAMPLE_REQUEST
    )

    assert response.status_code == 404
