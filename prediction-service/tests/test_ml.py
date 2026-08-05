import numpy as np
import pandas as pd
import pytest

from app.models.ml.feature_builder import TimeSeriesFeatureBuilder
from app.models.ml.linear_regression import LinearRegressionForecaster
from app.models.ml.random_forest import RandomForestForecaster
from app.models.ml.xgboost_forecaster import XGBoostForecaster


@pytest.fixture
def monthly_series() -> pd.Series:
    rng = np.random.default_rng(42)
    index = pd.date_range("2019-01-01", periods=48, freq="ME")
    trend = np.arange(48) * 5.0
    seasonality = np.tile([0, 10, 20, 30, 25, 15, 5, -5, -10, -5, 0, 5], 4)
    noise = rng.normal(0, 3, 48)
    return pd.Series(200 + trend + seasonality + noise, index=index)


# ---------- feature builder ----------

def test_feature_builder_output_shape(monthly_series):
    fb = TimeSeriesFeatureBuilder(n_lags=6)
    X, y = fb.fit_transform(monthly_series)
    # n - n_lags linhas de treino
    assert len(X) == len(monthly_series) - 6
    assert len(y) == len(monthly_series) - 6


def test_feature_builder_column_names(monthly_series):
    fb = TimeSeriesFeatureBuilder(n_lags=3, include_date_features=True)
    X, _ = fb.fit_transform(monthly_series)
    assert "lag_1" in X.columns
    assert "lag_3" in X.columns
    assert "rolling_mean_3" in X.columns
    assert "month" in X.columns
    assert "trend" in X.columns


def test_feature_builder_no_date_features(monthly_series):
    fb = TimeSeriesFeatureBuilder(n_lags=3, include_date_features=False)
    X, _ = fb.fit_transform(monthly_series)
    assert "month" not in X.columns
    assert "quarter" not in X.columns


def test_prediction_features_matches_training_columns(monthly_series):
    fb = TimeSeriesFeatureBuilder(n_lags=6)
    X_train, _ = fb.fit_transform(monthly_series)
    X_pred = fb.prediction_features(
        context=list(monthly_series.values),
        step=0,
        last_date=monthly_series.index[-1],
        freq=monthly_series.index.freq,
    )
    assert list(X_pred.columns) == list(X_train.columns)


# ---------- linear regression ----------

def test_linear_regression_length(monthly_series):
    result = LinearRegressionForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


def test_linear_regression_finite_values(monthly_series):
    result = LinearRegressionForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    assert all(np.isfinite(v) for v in result.predictions)


def test_linear_regression_future_index(monthly_series):
    result = LinearRegressionForecaster(n_lags=6).fit_predict(monthly_series, horizon=3)
    assert all(ts > monthly_series.index[-1] for ts in result.predictions.index)


def test_linear_regression_no_intervals(monthly_series):
    result = LinearRegressionForecaster(n_lags=6).fit_predict(monthly_series, horizon=3)
    assert result.lower_bound is None
    assert result.upper_bound is None


# ---------- random forest ----------

def test_random_forest_length(monthly_series):
    result = RandomForestForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


def test_random_forest_finite_values(monthly_series):
    result = RandomForestForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    assert all(np.isfinite(v) for v in result.predictions)


def test_random_forest_recursive_predictions_differ(monthly_series):
    result = RandomForestForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    # previsões recursivas não devem ser todas iguais numa série com tendência
    assert len(set(round(v, 2) for v in result.predictions)) > 1


# ---------- xgboost ----------

def test_xgboost_length(monthly_series):
    result = XGBoostForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


def test_xgboost_finite_values(monthly_series):
    result = XGBoostForecaster(n_lags=6).fit_predict(monthly_series, horizon=6)
    assert all(np.isfinite(v) for v in result.predictions)


def test_xgboost_future_index(monthly_series):
    result = XGBoostForecaster(n_lags=6).fit_predict(monthly_series, horizon=3)
    assert all(ts > monthly_series.index[-1] for ts in result.predictions.index)


# ---------- erros esperados ----------

def test_raises_if_too_few_observations():
    short = pd.Series(
        [1.0, 2.0, 3.0],
        index=pd.date_range("2023-01-01", periods=3, freq="ME"),
    )
    with pytest.raises(ValueError, match="n_lags"):
        LinearRegressionForecaster(n_lags=12).fit(short)


def test_large_n_lags_still_works(monthly_series):
    # n_lags=24, série de 48 → 24 amostras de treino
    result = LinearRegressionForecaster(n_lags=24).fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


# ---------- integração via API ----------

def test_api_linear_regression(client):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 60,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "linear_regression",
        "forecast_horizon": 3,
        "frequency": "ME",
        "n_lags": 6,
        "include_date_features": True,
    })
    dates = pd.date_range("2019-01-01", periods=48, freq="ME")
    records = [{"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 100)} for i, d in enumerate(dates)]
    r = client.post("/api/forecast", json={
        "pipelineId": 60, "pipelineName": "test_lr",
        "scheduleConfigId": 1, "batchExecutionId": 10,
        "recordCount": len(records), "records": records,
    })
    assert r.status_code == 200
    body = r.json()
    assert body["model_used"] == "linear_regression"
    assert len(body["predictions"]) == 3


def test_api_xgboost(client):
    client.post("/api/config/pipelines", json={
        "pipeline_id": 70,
        "date_field": "data",
        "target_fields": [{"field_name": "vendas"}],
        "model": "xgboost",
        "forecast_horizon": 3,
        "frequency": "ME",
        "n_lags": 6,
    })
    dates = pd.date_range("2019-01-01", periods=48, freq="ME")
    records = [{"data": d.strftime("%Y-%m-%d"), "vendas": str((i + 1) * 100)} for i, d in enumerate(dates)]
    r = client.post("/api/forecast", json={
        "pipelineId": 70, "pipelineName": "test_xgb",
        "scheduleConfigId": 1, "batchExecutionId": 11,
        "recordCount": len(records), "records": records,
    })
    assert r.status_code == 200
    assert r.json()["model_used"] == "xgboost"
