import pandas as pd

from app.models.base import BaseForecaster, ForecastResult


class NaiveForecaster(BaseForecaster):
    """Forecast = último valor observado repetido para todos os horizontes."""

    def fit(self, series: pd.Series) -> "NaiveForecaster":
        self._last_value = float(series.iloc[-1])
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict(self, horizon: int) -> ForecastResult:
        idx = self._future_index(horizon)
        return ForecastResult(
            predictions=pd.Series([self._last_value] * horizon, index=idx)
        )

    @property
    def name(self) -> str:
        return "naive"
