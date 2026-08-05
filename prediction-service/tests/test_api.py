import pandas as pd
import pytest
from fastapi.testclient import TestClient


def _monthly_records(n: int = 24, extra_field: str = None) -> list[dict]:
    dates = pd.date_range("2022-01-01", periods=n, freq="ME")
    records = [{"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 100)} for i, d in enumerate(dates)]
    if extra_field:
        for i, r in enumerate(records):
            r[extra_field] = str((i + 1) * 10)
    return records


# ---------- infra ----------

def test_health(client: TestClient):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "up"


def test_list_models(client: TestClient):
    r = client.get("/api/models")
    assert r.status_code == 200
    names = {m["name"] for m in r.json()}
    assert names == {"naive", "mean", "drift", "seasonal_naive", "ses", "holtwinters", "arima"}


# ---------- CRUD configurações ----------

def test_create_and_get_config(client: TestClient):
    r = client.post("/api/config/pipelines", json={
        "pipeline_id": 1,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "naive",
        "frequency": "ME",
    })
    assert r.status_code == 201
    assert r.json()["model"] == "naive"
    assert r.json()["target_fields"][0]["field_name"] == "vendas"


def test_update_config_is_idempotent(client: TestClient):
    payload = {"pipeline_id": 2, "date_field": "data", "target_fields": [{"field_name": "v"}], "model": "naive"}
    client.post("/api/config/pipelines", json=payload)
    payload["model"] = "mean"
    r = client.post("/api/config/pipelines", json=payload)
    assert r.status_code == 201
    assert client.get("/api/config/pipelines/2").json()["model"] == "mean"


def test_get_config_not_found(client: TestClient):
    assert client.get("/api/config/pipelines/999").status_code == 404


def test_list_configs(client: TestClient):
    for i in range(3):
        client.post("/api/config/pipelines", json={
            "pipeline_id": i + 10,
            "date_field": "data",
            "target_fields": [{"field_name": "vendas"}],
        })
    assert len(client.get("/api/config/pipelines").json()) == 3


def test_delete_config(client: TestClient):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 5, "date_field": "data", "target_fields": [{"field_name": "v"}],
    })
    assert client.delete("/api/config/pipelines/5").status_code == 204
    assert client.get("/api/config/pipelines/5").status_code == 404


def test_delete_not_found(client: TestClient):
    assert client.delete("/api/config/pipelines/999").status_code == 404


# ---------- previsão — caso base ----------

def test_forecast_no_config_returns_404(client: TestClient):
    payload = {
        "pipelineId": 999, "pipelineName": "x",
        "scheduleConfigId": 1, "batchExecutionId": 1,
        "recordCount": 1, "records": [{"data": "2023-01-01", "vendas": "100"}],
    }
    assert client.post("/api/forecast", json=payload).status_code == 404


def test_forecast_naive_single_field(client: TestClient):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 10,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "naive",
        "forecast_horizon": 3,
        "frequency": "ME",
    })
    r = client.post("/api/forecast", json={
        "pipelineId": 10, "pipelineName": "test_naive",
        "scheduleConfigId": 1, "batchExecutionId": 1,
        "recordCount": 24, "records": _monthly_records(24),
    })
    assert r.status_code == 200
    body = r.json()
    assert body["model_used"] == "naive"
    assert body["n_training_series"] == 1
    assert len(body["predictions"]) == 3
    assert all(p["value"] == 2400.0 for p in body["predictions"])
    assert all(p["target_field"] == "vendas" for p in body["predictions"])
    assert all(p["group"] is None for p in body["predictions"])


# ---------- múltiplos campos alvo ----------

def test_forecast_multiple_target_fields(client: TestClient):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 20,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}, {"field_name": "quantidade"}],
        "model": "naive",
        "forecast_horizon": 2,
        "frequency": "ME",
    })
    r = client.post("/api/forecast", json={
        "pipelineId": 20, "pipelineName": "test_multi",
        "scheduleConfigId": 1, "batchExecutionId": 2,
        "recordCount": 24, "records": _monthly_records(24, extra_field="quantidade"),
    })
    assert r.status_code == 200
    body = r.json()
    assert len(body["predictions"]) == 4          # 2 campos × 2 horizonte
    assert body["n_training_series"] == 2
    assert {p["target_field"] for p in body["predictions"]} == {"vendas", "quantidade"}


# ---------- agrupamento ----------

def test_forecast_with_group_field(client: TestClient):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 30,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "group_field": "produto",
        "model": "naive",
        "forecast_horizon": 2,
        "frequency": "ME",
    })
    dates = pd.date_range("2022-01-01", periods=12, freq="ME")
    records = []
    for i, d in enumerate(dates):
        records.append({"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 100), "produto": "A"})
        records.append({"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 50),  "produto": "B"})

    r = client.post("/api/forecast", json={
        "pipelineId": 30, "pipelineName": "test_group",
        "scheduleConfigId": 1, "batchExecutionId": 3,
        "recordCount": len(records), "records": records,
    })
    assert r.status_code == 200
    body = r.json()
    assert len(body["predictions"]) == 4          # 1 campo × 2 grupos × 2 horizonte
    assert body["n_training_series"] == 2
    assert {p["group"] for p in body["predictions"]} == {"A", "B"}


# ---------- modelos estatísticos ----------

def test_forecast_holtwinters(client: TestClient):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 40,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "holtwinters",
        "forecast_horizon": 6,
        "frequency": "ME",
        "season_period": 12,
    })
    r = client.post("/api/forecast", json={
        "pipelineId": 40, "pipelineName": "test_hw",
        "scheduleConfigId": 1, "batchExecutionId": 4,
        "recordCount": 36, "records": _monthly_records(36),
    })
    assert r.status_code == 200
    assert r.json()["model_used"] == "holtwinters"
    assert len(r.json()["predictions"]) == 6


def test_forecast_arima_has_intervals(client: TestClient):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 50,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "arima",
        "forecast_horizon": 4,
        "frequency": "ME",
        "arima_order": [1, 1, 1],
    })
    r = client.post("/api/forecast", json={
        "pipelineId": 50, "pipelineName": "test_arima",
        "scheduleConfigId": 1, "batchExecutionId": 5,
        "recordCount": 36, "records": _monthly_records(36),
    })
    assert r.status_code == 200
    preds = r.json()["predictions"]
    assert all(p["lower_bound"] is not None for p in preds)
    assert all(p["upper_bound"] is not None for p in preds)
