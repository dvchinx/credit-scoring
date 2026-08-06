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


def test_health_reports_model_loaded(client):
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["model_loaded"] is True
    assert body["model_version"]


def test_health_degraded_without_trained_model(empty_artifacts_client):
    response = empty_artifacts_client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "degraded"
    assert body["model_loaded"] is False
    assert body["model_version"] is None


def test_score_returns_probability_and_risk_score(client):
    response = client.post("/score", json=SAMPLE_REQUEST)

    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["probability_of_default"] <= 1.0
    assert 300 <= body["risk_score"] <= 850
    assert body["model_version"]


def test_score_rejects_incomplete_payload(client):
    incomplete_request = dict(SAMPLE_REQUEST)
    del incomplete_request["age"]

    response = client.post("/score", json=incomplete_request)

    assert response.status_code == 422


def test_score_rejects_negative_values(client):
    invalid_request = dict(SAMPLE_REQUEST, debt_ratio=-1)

    response = client.post("/score", json=invalid_request)

    assert response.status_code == 422


def test_score_returns_503_when_model_not_loaded(empty_artifacts_client):
    response = empty_artifacts_client.post("/score", json=SAMPLE_REQUEST)

    assert response.status_code == 503
