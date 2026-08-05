from xgboost import XGBRegressor

from app.models.ml.base_ml import BaseMLForecaster


class XGBoostForecaster(BaseMLForecaster):
    """XGBoost para séries temporais.

    Gradient boosting de árvores — excelente performance em dados tabulares,
    trata valores em falta nativamente e é resistente a overfitting via
    regularização L1/L2 incorporada.
    """

    def __init__(
        self,
        n_lags: int = 12,
        include_date_features: bool = True,
        n_estimators: int = 100,
        learning_rate: float = 0.1,
        max_depth: int = 6,
    ) -> None:
        super().__init__(n_lags, include_date_features)
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

    @property
    def name(self) -> str:
        return "xgboost"
