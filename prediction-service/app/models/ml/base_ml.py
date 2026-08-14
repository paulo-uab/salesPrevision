from abc import abstractmethod
from typing import Any, Optional

import pandas as pd

from app.models.base import BaseForecaster, ForecastResult
from app.models.ml.feature_builder import TimeSeriesFeatureBuilder


class BaseMLForecaster(BaseForecaster):
    """Base para modelos ML supervisionados de séries temporais.

    Estratégia de previsão multi-passo: recursiva — cada previsão é
    adicionada ao contexto antes de prever o passo seguinte.
    Os modelos não produzem intervalos de confiança (lower/upper = None).
    """

    supports_exog = True

    def __init__(self, n_lags: int = 12, include_date_features: bool = True, frequency: str = "ME") -> None:
        self._n_lags = n_lags
        self._include_date_features = include_date_features
        self._feature_builder = TimeSeriesFeatureBuilder(n_lags, include_date_features, frequency)
        self._model: Any = None
        self._exog_last_values: dict[str, float] = {}

    @abstractmethod
    def _create_model(self) -> Any:
        """Instancia e devolve o modelo sklearn/xgboost."""

    def fit(self, series: pd.Series) -> "BaseMLForecaster":
        self._validate_length(series)
        self._training_values = list(series.values.astype(float))
        self._training_series = series
        self._last_date = series.index[-1]
        self._freq = series.index.freq

        X, y = self._feature_builder.fit_transform(series)
        self._model = self._create_model()
        self._model.fit(X, y)
        return self

    def predict(self, horizon: int) -> ForecastResult:
        return self._recursive_predict(horizon, exog_values=None)

    def fit_with_exog(self, series: pd.Series, exog: pd.DataFrame) -> "BaseMLForecaster":
        self._validate_length(series)
        self._training_values = list(series.values.astype(float))
        self._training_series = series
        self._last_date = series.index[-1]
        self._freq = series.index.freq
        # Sem canal para valores futuros conhecidos das exógenas nesta versão —
        # assume-se o último valor observado, constante, para todo o horizonte.
        self._exog_last_values = {col: float(exog[col].iloc[-1]) for col in exog.columns}

        X, y = self._feature_builder.fit_transform(series, exog=exog)
        self._model = self._create_model()
        self._model.fit(X, y)
        return self

    def predict_with_exog(self, horizon: int, future_exog: Optional[pd.DataFrame] = None) -> ForecastResult:
        return self._recursive_predict(horizon, exog_values=self._exog_last_values)

    def _validate_length(self, series: pd.Series) -> None:
        if len(series) <= self._n_lags:
            raise ValueError(
                f"Série tem {len(series)} observações mas n_lags={self._n_lags}. "
                f"São necessárias pelo menos {self._n_lags + 1} observações."
            )

    def _recursive_predict(self, horizon: int, exog_values: Optional[dict[str, float]]) -> ForecastResult:
        context = list(self._training_values)
        predictions: list[float] = []

        for step in range(horizon):
            X_pred = self._feature_builder.prediction_features(
                context=context,
                step=step,
                last_date=self._last_date,
                freq=self._freq,
                exog_values=exog_values,
            )
            pred = float(self._model.predict(X_pred)[0])
            predictions.append(pred)
            context.append(pred)

        return ForecastResult(predictions=pd.Series(predictions, index=self._future_index(horizon)))
