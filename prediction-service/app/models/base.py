from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Any, Optional

import pandas as pd


@dataclass
class ForecastResult:
    predictions: pd.Series
    lower_bound: Optional[pd.Series] = field(default=None)
    upper_bound: Optional[pd.Series] = field(default=None)


class BaseForecaster(ABC):

    # True apenas nos forecasters que sabem incorporar pontos novos num modelo
    # já ajustado (ver get_state/set_state/update), sem reajustar do zero.
    supports_incremental: bool = False

    # True apenas nos forecasters que sabem usar variáveis exógenas (ver
    # fit_with_exog/predict_with_exog). Os restantes ignoram-nas silenciosamente.
    supports_exog: bool = False

    @abstractmethod
    def fit(self, series: pd.Series) -> "BaseForecaster":
        pass

    @abstractmethod
    def predict(self, horizon: int) -> ForecastResult:
        pass

    def fit_predict(self, series: pd.Series, horizon: int) -> ForecastResult:
        return self.fit(series).predict(horizon)

    def get_state(self) -> Any:
        """Devolve o estado interno a persistir para uma atualização incremental
        futura. Só chamado quando supports_incremental=True."""
        raise NotImplementedError(f"{self.name} não suporta treino incremental")

    def set_state(self, state: Any, last_date: pd.Timestamp, freq) -> None:
        """Restaura o estado de uma execução anterior antes de update()."""
        raise NotImplementedError(f"{self.name} não suporta treino incremental")

    def update(self, new_points: pd.Series, horizon: int) -> ForecastResult:
        """Incorpora só os pontos ainda não vistos (new_points pode ser vazia,
        se não houver dados novos desde a última execução) no modelo já
        restaurado via set_state(), sem reajustar do zero."""
        raise NotImplementedError(f"{self.name} não suporta treino incremental")

    def fit_with_exog(self, series: pd.Series, exog: pd.DataFrame) -> "BaseForecaster":
        """Equivalente a fit(), mas incorporando variáveis exógenas alinhadas
        ao mesmo índice temporal da série alvo. Só chamado quando supports_exog=True."""
        raise NotImplementedError(f"{self.name} não suporta variáveis exógenas")

    def predict_with_exog(self, horizon: int, future_exog: Optional[pd.DataFrame] = None) -> ForecastResult:
        """Equivalente a predict(), mas recebendo os valores futuros (assumidos)
        das variáveis exógenas para o horizonte de previsão. Quando future_exog
        não é fornecido (não há canal para valores futuros conhecidos nesta
        versão), a implementação assume o último valor observado, constante."""
        raise NotImplementedError(f"{self.name} não suporta variáveis exógenas")

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
