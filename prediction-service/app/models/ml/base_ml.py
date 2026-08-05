from abc import abstractmethod
from typing import Any

import pandas as pd

from app.models.base import BaseForecaster, ForecastResult
from app.models.ml.feature_builder import TimeSeriesFeatureBuilder


class BaseMLForecaster(BaseForecaster):
    """Base para modelos ML supervisionados de séries temporais.

    Estratégia de previsão multi-passo: recursiva — cada previsão é
    adicionada ao contexto antes de prever o passo seguinte.
    Os modelos não produzem intervalos de confiança (lower/upper = None).
    """

    def __init__(self, n_lags: int = 12, include_date_features: bool = True) -> None:
        self._n_lags = n_lags
        self._include_date_features = include_date_features
        self._feature_builder = TimeSeriesFeatureBuilder(n_lags, include_date_features)
        self._model: Any = None

    @abstractmethod
    def _create_model(self) -> Any:
        """Instancia e devolve o modelo sklearn/xgboost."""

    def fit(self, series: pd.Series) -> "BaseMLForecaster":
        if len(series) <= self._n_lags:
            raise ValueError(
                f"Série tem {len(series)} observações mas n_lags={self._n_lags}. "
                f"São necessárias pelo menos {self._n_lags + 1} observações."
            )

        self._training_values = list(series.values.astype(float))
        self._last_date = series.index[-1]
        self._freq = series.index.freq

        X, y = self._feature_builder.fit_transform(series)
        self._model = self._create_model()
        self._model.fit(X, y)
        return self

    def predict(self, horizon: int) -> ForecastResult:
        context = list(self._training_values)
        predictions: list[float] = []

        for step in range(horizon):
            X_pred = self._feature_builder.prediction_features(
                context=context,
                step=step,
                last_date=self._last_date,
                freq=self._freq,
            )
            pred = float(self._model.predict(X_pred)[0])
            predictions.append(pred)
            context.append(pred)

        return ForecastResult(predictions=pd.Series(predictions, index=self._future_index(horizon)))
