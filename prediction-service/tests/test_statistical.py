import numpy as np
import pandas as pd
import pytest

from app.models.statistical.arima import ARIMAForecaster
from app.models.statistical.holtwinters import HoltWintersForecaster
from app.models.statistical.ses import SESForecaster


@pytest.fixture
def monthly_series() -> pd.Series:
    rng = np.random.default_rng(42)
    index = pd.date_range("2020-01-01", periods=48, freq="ME")
    trend = np.arange(48) * 5.0
    seasonality = np.tile([0, 10, 20, 30, 25, 15, 5, -5, -10, -5, 0, 5], 4)
    noise = rng.normal(0, 3, 48)
    return pd.Series(200 + trend + seasonality + noise, index=index)


def test_ses_length(monthly_series):
    result = SESForecaster().fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


def test_ses_finite_values(monthly_series):
    result = SESForecaster().fit_predict(monthly_series, horizon=6)
    assert all(np.isfinite(v) for v in result.predictions)


def test_ses_no_intervals(monthly_series):
    result = SESForecaster().fit_predict(monthly_series, horizon=6)
    assert result.lower_bound is None
    assert result.upper_bound is None


def test_holtwinters_with_seasonality(monthly_series):
    result = HoltWintersForecaster(season_period=12).fit_predict(monthly_series, horizon=12)
    assert len(result.predictions) == 12
    assert all(np.isfinite(v) for v in result.predictions)


def test_holtwinters_degrades_without_enough_data():
    short = pd.Series(
        range(1, 16),
        index=pd.date_range("2023-01-01", periods=15, freq="ME"),
        dtype=float,
    )
    # 15 < 2*12 → deve usar apenas tendência (sem sazonalidade) sem levantar erro
    result = HoltWintersForecaster(season_period=12).fit_predict(short, horizon=6)
    assert len(result.predictions) == 6


def test_holtwinters_no_season_period(monthly_series):
    result = HoltWintersForecaster(season_period=None).fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


def test_arima_length(monthly_series):
    result = ARIMAForecaster(order=(1, 1, 1)).fit_predict(monthly_series, horizon=6)
    assert len(result.predictions) == 6


def test_arima_has_intervals(monthly_series):
    result = ARIMAForecaster(order=(1, 1, 1)).fit_predict(monthly_series, horizon=6)
    assert result.lower_bound is not None
    assert result.upper_bound is not None
    assert len(result.lower_bound) == 6
    assert len(result.upper_bound) == 6


def test_arima_intervals_ordered(monthly_series):
    result = ARIMAForecaster(order=(1, 1, 1)).fit_predict(monthly_series, horizon=6)
    assert all(
        lo <= pred <= hi
        for lo, pred, hi in zip(result.lower_bound, result.predictions, result.upper_bound)
    )
