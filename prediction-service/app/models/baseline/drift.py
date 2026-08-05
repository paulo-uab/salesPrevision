import pandas as pd

from app.models.base import BaseForecaster, ForecastResult


class DriftForecaster(BaseForecaster):
    """Extrapolação linear: tendência do primeiro ao último ponto observado."""

    def fit(self, series: pd.Series) -> "DriftForecaster":
        n = len(series)
        self._slope = (float(series.iloc[-1]) - float(series.iloc[0])) / (n - 1) if n > 1 else 0.0
        self._last_value = float(series.iloc[-1])
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict(self, horizon: int) -> ForecastResult:
        idx = self._future_index(horizon)
        values = [self._last_value + self._slope * h for h in range(1, horizon + 1)]
        return ForecastResult(predictions=pd.Series(values, index=idx))

    @property
    def name(self) -> str:
        return "drift"
