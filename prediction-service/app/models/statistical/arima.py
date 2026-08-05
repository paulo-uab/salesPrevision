from typing import Optional

import pandas as pd
from statsmodels.tsa.arima.model import ARIMA as StatsARIMA

from app.models.base import BaseForecaster, ForecastResult


class ARIMAForecaster(BaseForecaster):
    """ARIMA / SARIMA com intervalos de confiança a 95%."""

    def __init__(
        self,
        order: tuple[int, int, int] = (1, 1, 1),
        seasonal_order: Optional[tuple[int, int, int, int]] = None,
    ) -> None:
        self._order = order
        # Apenas passa seasonal_order se o período s for > 1
        self._seasonal_order = seasonal_order if (seasonal_order and seasonal_order[3] > 1) else None

    def fit(self, series: pd.Series) -> "ARIMAForecaster":
        kwargs: dict = {"order": self._order}
        if self._seasonal_order:
            kwargs["seasonal_order"] = self._seasonal_order

        self._result = StatsARIMA(series, **kwargs).fit(disp=False)
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict(self, horizon: int) -> ForecastResult:
        forecast = self._result.get_forecast(steps=horizon)
        mean = forecast.predicted_mean
        ci = forecast.conf_int(alpha=0.05)

        idx = self._future_index(horizon)
        return ForecastResult(
            predictions=pd.Series(mean.values, index=idx),
            lower_bound=pd.Series(ci.iloc[:, 0].values, index=idx),
            upper_bound=pd.Series(ci.iloc[:, 1].values, index=idx),
        )

    @property
    def name(self) -> str:
        return "arima"
