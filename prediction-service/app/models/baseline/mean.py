import pandas as pd

from app.models.base import BaseForecaster, ForecastResult


class MeanForecaster(BaseForecaster):
    """Forecast = média de todos os valores históricos."""

    def fit(self, series: pd.Series) -> "MeanForecaster":
        self._mean = float(series.mean())
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict(self, horizon: int) -> ForecastResult:
        idx = self._future_index(horizon)
        return ForecastResult(
            predictions=pd.Series([self._mean] * horizon, index=idx)
        )

    @property
    def name(self) -> str:
        return "mean"
