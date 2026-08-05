import pandas as pd
import pytest

from app.models.baseline.drift import DriftForecaster
from app.models.baseline.mean import MeanForecaster
from app.models.baseline.naive import NaiveForecaster
from app.models.baseline.seasonal_naive import SeasonalNaiveForecaster


@pytest.fixture
def monthly_series() -> pd.Series:
    index = pd.date_range("2023-01-01", periods=24, freq="ME")
    return pd.Series(range(1, 25), index=index, dtype=float)


def test_naive_returns_last_value(monthly_series):
    result = NaiveForecaster().fit_predict(monthly_series, horizon=3)
    assert len(result.predictions) == 3
    assert list(result.predictions) == [24.0, 24.0, 24.0]


def test_naive_future_index_is_correct(monthly_series):
    result = NaiveForecaster().fit_predict(monthly_series, horizon=2)
    assert result.predictions.index[0] > monthly_series.index[-1]


def test_mean_returns_average(monthly_series):
    result = MeanForecaster().fit_predict(monthly_series, horizon=3)
    expected = sum(range(1, 25)) / 24  # 12.5
    assert len(result.predictions) == 3
    assert all(abs(v - expected) < 1e-9 for v in result.predictions)


def test_drift_linear_extrapolation(monthly_series):
    # slope = (24-1)/(24-1) = 1.0 → next values: 25, 26, 27
    result = DriftForecaster().fit_predict(monthly_series, horizon=3)
    assert abs(result.predictions.iloc[0] - 25.0) < 1e-9
    assert abs(result.predictions.iloc[1] - 26.0) < 1e-9
    assert abs(result.predictions.iloc[2] - 27.0) < 1e-9


def test_drift_flat_series():
    flat = pd.Series([5.0] * 10, index=pd.date_range("2023-01-01", periods=10, freq="ME"))
    result = DriftForecaster().fit_predict(flat, horizon=3)
    assert all(v == 5.0 for v in result.predictions)


def test_seasonal_naive_correct_values(monthly_series):
    # n=24, m=12: h=1 → series[12]=13, h=2 → series[13]=14, h=3 → series[14]=15
    result = SeasonalNaiveForecaster(season_period=12).fit_predict(monthly_series, horizon=3)
    assert result.predictions.iloc[0] == 13.0
    assert result.predictions.iloc[1] == 14.0
    assert result.predictions.iloc[2] == 15.0


def test_seasonal_naive_wraps_after_full_season(monthly_series):
    # h=13 deve repetir h=1 (wrap)
    result = SeasonalNaiveForecaster(season_period=12).fit_predict(monthly_series, horizon=13)
    assert result.predictions.iloc[12] == result.predictions.iloc[0]


def test_seasonal_naive_raises_if_too_short():
    short = pd.Series([1.0, 2.0, 3.0], index=pd.date_range("2023-01-01", periods=3, freq="ME"))
    with pytest.raises(ValueError, match="season_period"):
        SeasonalNaiveForecaster(season_period=12).fit(short)
