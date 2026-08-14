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


def _request(pipeline_id: int, records: list[dict], config: dict, **overrides) -> dict:
    payload = {
        "pipelineId": pipeline_id,
        "pipelineName": f"test_{pipeline_id}",
        "scheduleConfigId": 1,
        "batchExecutionId": pipeline_id,
        "recordCount": len(records),
        "records": records,
        "config": config,
    }
    payload.update(overrides)
    return payload


# ---------- infra ----------

def test_health(client: TestClient):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "up"


def test_list_models(client: TestClient):
    r = client.get("/api/models")
    assert r.status_code == 200
    names = {m["name"] for m in r.json()}
    assert names == {
        "naive", "mean", "drift", "seasonal_naive", "ses", "holtwinters", "arima",
        "linear_regression", "random_forest", "xgboost",
    }


# ---------- validação do pedido ----------

def test_forecast_missing_config_returns_422(client: TestClient):
    payload = {
        "pipelineId": 1, "pipelineName": "x",
        "scheduleConfigId": 1, "batchExecutionId": 1,
        "recordCount": 1, "records": [{"data": "2023-01-01", "vendas": "100"}],
        # "config" em falta de propósito
    }
    assert client.post("/api/forecast", json=payload).status_code == 422


def test_forecast_missing_date_field_returns_422(client: TestClient):
    payload = _request(2, _monthly_records(24), config={
        "target_fields": [{"field_name": "vendas"}],
        # "date_field" em falta de propósito
    })
    assert client.post("/api/forecast", json=payload).status_code == 422


# ---------- previsão — caso base ----------

def test_forecast_naive_single_field(client: TestClient):
    r = client.post("/api/forecast", json=_request(10, _monthly_records(24), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "naive",
        "control_model": "naive",
        "forecast_horizon": 3,
        "frequency": "ME",
    }))
    assert r.status_code == 200
    body = r.json()
    assert body["model_used"] == "naive"
    assert body["control_model_used"] == "naive"
    assert body["n_training_series"] == 1
    # model == control_model → não duplica as previsões
    assert len(body["predictions"]) == 3
    assert all(p["value"] == 2400.0 for p in body["predictions"])
    assert all(p["target_field"] == "vendas" for p in body["predictions"])
    assert all(p["group"] is None for p in body["predictions"])
    assert all(p["model"] == "naive" for p in body["predictions"])


# ---------- múltiplos campos alvo ----------

def test_forecast_multiple_target_fields(client: TestClient):
    r = client.post("/api/forecast", json=_request(20, _monthly_records(24, extra_field="quantidade"), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}, {"field_name": "quantidade"}],
        "model": "naive",
        "control_model": "naive",
        "forecast_horizon": 2,
        "frequency": "ME",
    }))
    assert r.status_code == 200
    body = r.json()
    assert len(body["predictions"]) == 4          # 2 campos × 2 horizonte
    assert body["n_training_series"] == 2
    assert {p["target_field"] for p in body["predictions"]} == {"vendas", "quantidade"}


# ---------- agrupamento ----------

def test_forecast_with_group_field(client: TestClient):
    dates = pd.date_range("2022-01-01", periods=12, freq="ME")
    records = []
    for i, d in enumerate(dates):
        records.append({"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 100), "produto": "A"})
        records.append({"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 50),  "produto": "B"})

    r = client.post("/api/forecast", json=_request(30, records, config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "group_field": "produto",
        "model": "naive",
        "control_model": "naive",
        "forecast_horizon": 2,
        "frequency": "ME",
    }))
    assert r.status_code == 200
    body = r.json()
    assert len(body["predictions"]) == 4          # 1 campo × 2 grupos × 2 horizonte
    assert body["n_training_series"] == 2
    assert {p["group"] for p in body["predictions"]} == {"A", "B"}


# ---------- modelos estatísticos ----------

def test_forecast_holtwinters(client: TestClient):
    r = client.post("/api/forecast", json=_request(40, _monthly_records(36), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "holtwinters",
        "control_model": "holtwinters",
        "forecast_horizon": 6,
        "frequency": "ME",
        "season_period": 12,
    }))
    assert r.status_code == 200
    assert r.json()["model_used"] == "holtwinters"
    assert len(r.json()["predictions"]) == 6


def test_forecast_arima_has_intervals(client: TestClient):
    r = client.post("/api/forecast", json=_request(50, _monthly_records(36), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "arima",
        "control_model": "arima",
        "forecast_horizon": 4,
        "frequency": "ME",
        "arima_order": [1, 1, 1],
    }))
    assert r.status_code == 200
    preds = r.json()["predictions"]
    assert all(p["lower_bound"] is not None for p in preds)
    assert all(p["upper_bound"] is not None for p in preds)


# ---------- modelo de controlo ----------

def test_control_model_runs_alongside_main_model(client: TestClient):
    r = client.post("/api/forecast", json=_request(60, _monthly_records(36), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "holtwinters",
        "control_model": "naive",
        "forecast_horizon": 4,
        "frequency": "ME",
        "season_period": 12,
    }))
    assert r.status_code == 200
    body = r.json()
    assert body["model_used"] == "holtwinters"
    assert body["control_model_used"] == "naive"
    # dois modelos diferentes → 2x os pontos (4 holtwinters + 4 naive)
    assert len(body["predictions"]) == 8
    models_seen = {p["model"] for p in body["predictions"]}
    assert models_seen == {"holtwinters", "naive"}
    holtwinters_points = [p for p in body["predictions"] if p["model"] == "holtwinters"]
    naive_points = [p for p in body["predictions"] if p["model"] == "naive"]
    assert len(holtwinters_points) == 4
    assert len(naive_points) == 4
    # naive não produz intervalos de confiança, holtwinters também não (não é ARIMA)
    assert all(p["lower_bound"] is None for p in naive_points)


def test_control_model_defaults_to_naive(client: TestClient):
    r = client.post("/api/forecast", json=_request(61, _monthly_records(24), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "mean",
        # control_model omitido — deve assumir "naive" por omissão
        "forecast_horizon": 3,
        "frequency": "ME",
    }))
    assert r.status_code == 200
    assert r.json()["control_model_used"] == "naive"


# ---------- variáveis exógenas ----------

def _records_with_promo(n: int = 36) -> list[dict]:
    dates = pd.date_range("2019-01-01", periods=n, freq="ME")
    return [
        {"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 100), "promo": str(i % 2)}
        for i, d in enumerate(dates)
    ]


def test_forecast_with_exog_field_arima(client: TestClient):
    r = client.post("/api/forecast", json=_request(70, _records_with_promo(36), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "exog_fields": [{"field_name": "promo", "aggregation": "max"}],
        "model": "arima",
        "control_model": "arima",
        "forecast_horizon": 3,
        "frequency": "ME",
        "arima_order": [1, 1, 0],
    }))
    assert r.status_code == 200
    body = r.json()
    assert len(body["predictions"]) == 3
    assert all(p["value"] is not None for p in body["predictions"])


def test_forecast_with_exog_field_random_forest(client: TestClient):
    r = client.post("/api/forecast", json=_request(71, _records_with_promo(36), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "exog_fields": [{"field_name": "promo", "aggregation": "max"}],
        "model": "random_forest",
        "control_model": "random_forest",
        "forecast_horizon": 3,
        "frequency": "ME",
        "n_lags": 6,
    }))
    assert r.status_code == 200
    assert len(r.json()["predictions"]) == 3


def test_forecast_without_exog_fields_still_works(client: TestClient):
    # exog_fields omitido — comportamento anterior inalterado (default [])
    r = client.post("/api/forecast", json=_request(72, _monthly_records(24), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "naive",
        "control_model": "naive",
        "forecast_horizon": 3,
        "frequency": "ME",
    }))
    assert r.status_code == 200
    assert len(r.json()["predictions"]) == 3


def test_exog_ignored_for_model_without_support(client: TestClient):
    # naive não suporta exog — a config é aceite, o campo é simplesmente ignorado
    r = client.post("/api/forecast", json=_request(73, _records_with_promo(24), config={
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "exog_fields": [{"field_name": "promo", "aggregation": "max"}],
        "model": "naive",
        "control_model": "naive",
        "forecast_horizon": 3,
        "frequency": "ME",
    }))
    assert r.status_code == 200
    assert len(r.json()["predictions"]) == 3
