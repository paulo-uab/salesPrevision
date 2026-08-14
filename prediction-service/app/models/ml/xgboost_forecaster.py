import pandas as pd
from xgboost import XGBRegressor

from app.models.base import ForecastResult
from app.models.ml.base_ml import BaseMLForecaster


class XGBoostForecaster(BaseMLForecaster):
    """XGBoost para séries temporais.

    Gradient boosting de árvores — excelente performance em dados tabulares,
    trata valores em falta nativamente e é resistente a overfitting via
    regularização L1/L2 incorporada.
    """

    supports_incremental = True

    def __init__(
        self,
        n_lags: int = 12,
        include_date_features: bool = True,
        frequency: str = "ME",
        n_estimators: int = 100,
        learning_rate: float = 0.1,
        max_depth: int = 6,
    ) -> None:
        super().__init__(n_lags, include_date_features, frequency)
        self._n_estimators = n_estimators
        self._learning_rate = learning_rate
        self._max_depth = max_depth

    def _create_model(self) -> XGBRegressor:
        return XGBRegressor(
            n_estimators=self._n_estimators,
            learning_rate=self._learning_rate,
            max_depth=self._max_depth,
            random_state=42,
            verbosity=0,
            n_jobs=-1,
        )

    def get_state(self):
        # get_booster() em vez do XGBRegressor completo — é o que xgb_model=
        # espera para continuar o boosting em update().
        return {"booster": self._model.get_booster(), "training_series": self._training_series}

    def set_state(self, state, last_date: pd.Timestamp, freq) -> None:
        self._training_series = state["training_series"]
        self._training_values = list(self._training_series.values.astype(float))
        self._last_date = last_date
        self._freq = freq
        self._previous_booster = state["booster"]
        self._model = self._create_model()

    def update(self, new_points: pd.Series, horizon: int) -> ForecastResult:
        # Continua o boosting a partir da árvore anterior (xgb_model=) em vez de
        # recomeçar do zero. Nota: cada update() acrescenta mais n_estimators
        # árvores ao modelo existente — cresce a cada chamada, não substitui.
        if len(new_points) > 0:
            full_series = pd.concat([self._training_series, new_points])
            X_full, y_full = self._feature_builder.fit_transform(full_series)
            n_new = len(new_points)
            self._model.fit(X_full.iloc[-n_new:], y_full.iloc[-n_new:], xgb_model=self._previous_booster)

            self._training_series = full_series
            self._training_values = list(full_series.values.astype(float))
            self._last_date = full_series.index[-1]

        return self.predict(horizon)

    @property
    def name(self) -> str:
        return "xgboost"
