import pandas as pd

from app.models.base import BaseForecaster, ForecastResult


class SeasonalNaiveForecaster(BaseForecaster):
    """Forecast = valor do mesmo período no ciclo anterior.

    Para h=1..m  → usa o último ciclo completo disponível.
    Para h>m     → repete o ciclo (wrap-around).
    """

    def __init__(self, season_period: int) -> None:
        self._season_period = season_period

    def fit(self, series: pd.Series) -> "SeasonalNaiveForecaster":
        if len(series) < self._season_period:
            raise ValueError(
                f"Série tem {len(series)} observações; o mínimo para "
                f"season_period={self._season_period} é {self._season_period}."
            )
        self._series = series.copy()
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        return self

    def predict(self, horizon: int) -> ForecastResult:
        n = len(self._series)
        m = self._season_period
        # Índice na série original: n-m + ((h-1) % m)
        # Ex: n=24, m=12, h=1 → índice 12 (mesmo mês do ano anterior)
        values = [
            float(self._series.iloc[n - m + ((h - 1) % m)])
            for h in range(1, horizon + 1)
        ]
        return ForecastResult(
            predictions=pd.Series(values, index=self._future_index(horizon))
        )

    @property
    def name(self) -> str:
        return "seasonal_naive"
