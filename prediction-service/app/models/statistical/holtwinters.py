from typing import Optional

import pandas as pd
from statsmodels.tsa.holtwinters import ExponentialSmoothing

from app.models.base import BaseForecaster, ForecastResult


class HoltWintersForecaster(BaseForecaster):
    """Suavização Exponencial Tripla: nível + tendência + sazonalidade aditiva.

    Se season_period for None ou a série for curta demais para dois ciclos
    completos, degrada automaticamente para Holt (tendência sem sazonalidade).
    """

    def __init__(self, season_period: Optional[int] = None) -> None:
        self._season_period = season_period

    def fit(self, series: pd.Series) -> "HoltWintersForecaster":
        m = self._season_period
        use_seasonal = m is not None and len(series) >= 2 * m

        if use_seasonal:
            model = ExponentialSmoothing(
                series,
                trend="add",
                seasonal="add",
                seasonal_periods=m,
                initialization_method="estimated",
            )
        else:
            model = ExponentialSmoothing(
                series,
                trend="add",
                seasonal=None,
                initialization_method="estimated",
            )

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
        return "holtwinters"
