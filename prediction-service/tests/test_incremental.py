from pathlib import Path

import numpy as np
import pandas as pd
import pytest

from app.config.settings import settings
from app.models.statistical.arima import ARIMAForecaster
from app.models.ml.xgboost_forecaster import XGBoostForecaster
from app.service import model_cache


@pytest.fixture
def monthly_series() -> pd.Series:
    index = pd.date_range("2020-01-01", periods=36, freq="ME")
    values = 100 + np.arange(36) * 2.0
    return pd.Series(values, index=index)


# ---------- model_cache ----------

def test_model_cache_missing_key_returns_none(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "MODEL_CACHE_DIR", str(tmp_path))
    assert model_cache.load_state(1, "vendas", None, "arima") is None


def test_model_cache_round_trip(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "MODEL_CACHE_DIR", str(tmp_path))
    model_cache.save_state(1, "vendas", None, "arima", {"last_date": "x", "forecaster_state": 42})
    loaded = model_cache.load_state(1, "vendas", None, "arima")
    assert loaded == {"last_date": "x", "forecaster_state": 42}


def test_model_cache_distinguishes_group_and_model(tmp_path, monkeypatch):
    monkeypatch.setattr(settings, "MODEL_CACHE_DIR", str(tmp_path))
    model_cache.save_state(1, "vendas", "A", "arima", {"v": 1})
    model_cache.save_state(1, "vendas", "B", "arima", {"v": 2})
    model_cache.save_state(1, "vendas", "A", "xgboost", {"v": 3})
    assert model_cache.load_state(1, "vendas", "A", "arima")["v"] == 1
    assert model_cache.load_state(1, "vendas", "B", "arima")["v"] == 2
    assert model_cache.load_state(1, "vendas", "A", "xgboost")["v"] == 3


# ---------- ARIMA: get_state/set_state/update ----------

def test_arima_update_incorporates_new_points(monthly_series):
    base_series = monthly_series.iloc[:30]
    new_points = monthly_series.iloc[30:]

    forecaster = ARIMAForecaster(order=(1, 1, 0))
    forecaster.fit(base_series)
    state = forecaster.get_state()

    restored = ARIMAForecaster(order=(1, 1, 0))
    restored.set_state(state, base_series.index[-1], base_series.index.freq)
    result = restored.update(new_points, horizon=3)

    assert len(result.predictions) == 3
    assert all(np.isfinite(v) for v in result.predictions)
    assert all(ts > new_points.index[-1] for ts in result.predictions.index)


def test_arima_update_with_no_new_points_just_repredicts(monthly_series):
    base_series = monthly_series.iloc[:30]

    forecaster = ARIMAForecaster(order=(1, 1, 0))
    forecaster.fit(base_series)
    state = forecaster.get_state()

    restored = ARIMAForecaster(order=(1, 1, 0))
    restored.set_state(state, base_series.index[-1], base_series.index.freq)
    empty = base_series.iloc[0:0]
    result = restored.update(empty, horizon=2)

    assert len(result.predictions) == 2


# ---------- XGBoost: get_state/set_state/update ----------

def test_xgboost_update_incorporates_new_points(monthly_series):
    base_series = monthly_series.iloc[:30]
    new_points = monthly_series.iloc[30:]

    forecaster = XGBoostForecaster(n_lags=6)
    forecaster.fit(base_series)
    state = forecaster.get_state()

    restored = XGBoostForecaster(n_lags=6)
    restored.set_state(state, base_series.index[-1], base_series.index.freq)
    result = restored.update(new_points, horizon=3)

    assert len(result.predictions) == 3
    assert all(np.isfinite(v) for v in result.predictions)
    assert all(ts > new_points.index[-1] for ts in result.predictions.index)


# ---------- integração via API ----------

def _records(dates: pd.DatetimeIndex) -> list[dict]:
    return [{"data": d.strftime("%Y-%m-%d"), "vendas": str(100 + i * 2)} for i, d in enumerate(dates)]


def test_incremental_forecast_creates_cache_file(client):
    dates = pd.date_range("2020-01-01", periods=36, freq="ME")
    r = client.post("/api/forecast", json={
        "pipelineId": 100, "pipelineName": "test_incremental",
        "scheduleConfigId": 1, "batchExecutionId": 1,
        "recordCount": 36, "records": _records(dates),
        "config": {
            "date_field": "data",
            "target_fields": [{"field_name": "vendas"}],
            "model": "arima",
            "control_model": "arima",
            "forecast_horizon": 3,
            "frequency": "ME",
            "arima_order": [1, 1, 0],
            "incremental_training": True,
        },
    })
    assert r.status_code == 200

    cache_files = list(Path(settings.MODEL_CACHE_DIR).glob("*.pkl"))
    assert len(cache_files) == 1


def test_incremental_forecast_second_call_with_more_data_still_works(client):
    dates = pd.date_range("2020-01-01", periods=36, freq="ME")
    config = {
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "xgboost",
        "control_model": "xgboost",
        "forecast_horizon": 3,
        "frequency": "ME",
        "n_lags": 6,
        "incremental_training": True,
    }

    r1 = client.post("/api/forecast", json={
        "pipelineId": 200, "pipelineName": "test_incremental_xgb",
        "scheduleConfigId": 1, "batchExecutionId": 1,
        "recordCount": 36, "records": _records(dates), "config": config,
    })
    assert r1.status_code == 200

    dates_extended = pd.date_range("2020-01-01", periods=39, freq="ME")
    r2 = client.post("/api/forecast", json={
        "pipelineId": 200, "pipelineName": "test_incremental_xgb",
        "scheduleConfigId": 1, "batchExecutionId": 2,
        "recordCount": 39, "records": _records(dates_extended), "config": config,
    })
    assert r2.status_code == 200
    assert len(r2.json()["predictions"]) == 3


def test_incremental_training_ignored_for_unsupported_model(client):
    dates = pd.date_range("2020-01-01", periods=24, freq="ME")
    r = client.post("/api/forecast", json={
        "pipelineId": 300, "pipelineName": "test_incremental_naive",
        "scheduleConfigId": 1, "batchExecutionId": 1,
        "recordCount": 24, "records": _records(dates),
        "config": {
            "date_field": "data",
            "target_fields": [{"field_name": "vendas"}],
            "model": "naive",
            "control_model": "naive",
            "forecast_horizon": 3,
            "frequency": "ME",
            "incremental_training": True,
        },
    })
    assert r.status_code == 200
    # naive não suporta incremental — a flag não deve ter criado cache nenhuma
    assert list(Path(settings.MODEL_CACHE_DIR).glob("*.pkl")) == []
