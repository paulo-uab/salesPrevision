from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Optional

import pandas as pd


@dataclass
class ForecastResult:
    predictions: pd.Series
    lower_bound: Optional[pd.Series] = field(default=None)
    upper_bound: Optional[pd.Series] = field(default=None)


class BaseForecaster(ABC):

    @abstractmethod
    def fit(self, series: pd.Series) -> "BaseForecaster":
        pass

    @abstractmethod
    def predict(self, horizon: int) -> ForecastResult:
        pass

    def fit_predict(self, series: pd.Series, horizon: int) -> ForecastResult:
        return self.fit(series).predict(horizon)

    @property
    @abstractmethod
    def name(self) -> str:
        pass

    def _future_index(self, horizon: int) -> pd.DatetimeIndex:
        return pd.date_range(
            start=self._last_date,
            periods=horizon + 1,
            freq=self._freq,
        )[1:]
