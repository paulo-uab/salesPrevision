from typing import Optional

import pandas as pd
from statsmodels.tsa.arima.model import ARIMA as StatsARIMA

from app.models.base import BaseForecaster, ForecastResult


class ARIMAForecaster(BaseForecaster):
    """ARIMA / SARIMA com intervalos de confiança a 95%."""

    supports_incremental = True
    # exog e incremental_training não se combinam nesta versão — quando há
    # variáveis exógenas, o modelo ajusta-se sempre do zero (ver forecast_service).
    supports_exog = True

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

    def fit_with_exog(self, series: pd.Series, exog: pd.DataFrame) -> "ARIMAForecaster":
        kwargs: dict = {"order": self._order}
        if self._seasonal_order:
            kwargs["seasonal_order"] = self._seasonal_order

        # exog pode ter cobertura temporal ligeiramente diferente da série alvo
        # (ex: dados de promoções recolhidos com outra cadência) — realinha ao
        # índice da série antes de passar ao statsmodels, que exige correspondência exata.
        exog_aligned = exog.reindex(series.index).ffill().bfill()
        self._exog_columns = list(exog_aligned.columns)
        self._exog_last_values = {col: float(exog_aligned[col].iloc[-1]) for col in exog_aligned.columns}
        self._result = StatsARIMA(series, exog=exog_aligned, **kwargs).fit(disp=False)
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict_with_exog(self, horizon: int, future_exog: Optional[pd.DataFrame] = None) -> ForecastResult:
        if future_exog is None:
            # Sem canal para valores futuros conhecidos nesta versão — assume-se
            # o último valor observado, constante, para todo o horizonte.
            future_exog = pd.DataFrame({col: [self._exog_last_values[col]] * horizon for col in self._exog_columns})

        forecast = self._result.get_forecast(steps=horizon, exog=future_exog[self._exog_columns])
        mean = forecast.predicted_mean
        ci = forecast.conf_int(alpha=0.05)

        idx = self._future_index(horizon)
        return ForecastResult(
            predictions=pd.Series(mean.values, index=idx),
            lower_bound=pd.Series(ci.iloc[:, 0].values, index=idx),
            upper_bound=pd.Series(ci.iloc[:, 1].values, index=idx),
        )

    def get_state(self):
        return self._result

    def set_state(self, state, last_date: pd.Timestamp, freq) -> None:
        self._result = state
        self._last_date = last_date
        self._freq = freq

    def update(self, new_points: pd.Series, horizon: int) -> ForecastResult:
        # append(refit=False) atualiza o filtro de Kalman com as observações
        # novas sem reestimar os parâmetros — muito mais barato que fit() do zero.
        if len(new_points) > 0:
            self._result = self._result.append(new_points, refit=False)
            self._last_date = new_points.index[-1]
        return self.predict(horizon)

    @property
    def name(self) -> str:
        return "arima"
