import pandas as pd
from statsmodels.tsa.holtwinters import SimpleExpSmoothing

from app.models.base import BaseForecaster, ForecastResult


class SESForecaster(BaseForecaster):
    """Suavização Exponencial Simples — captura nível sem tendência nem sazonalidade."""

    def fit(self, series: pd.Series) -> "SESForecaster":
        model = SimpleExpSmoothing(series, initialization_method="estimated")
        self._result = model.fit(optimized=True)
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict(self, horizon: int) -> ForecastResult:
        values = self._result.forecast(horizon)
        return ForecastResult(
            predictions=pd.Series(values, index=self._future_index(horizon))
        )

    @property
    def name(self) -> str:
        return "ses"
