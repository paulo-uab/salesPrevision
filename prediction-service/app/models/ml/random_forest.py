from sklearn.ensemble import RandomForestRegressor

from app.models.ml.base_ml import BaseMLForecaster


class RandomForestForecaster(BaseMLForecaster):
    """Random Forest para séries temporais.

    Robusto a outliers e não-linearidades; não requer standardização.
    O parâmetro n_estimators controla o trade-off velocidade/precisão.
    """

    def __init__(
        self,
        n_lags: int = 12,
        include_date_features: bool = True,
        frequency: str = "ME",
        n_estimators: int = 100,
        max_depth: int | None = None,
    ) -> None:
        super().__init__(n_lags, include_date_features, frequency)
        self._n_estimators = n_estimators
        self._max_depth = max_depth

    def _create_model(self) -> RandomForestRegressor:
        return RandomForestRegressor(
            n_estimators=self._n_estimators,
            max_depth=self._max_depth,
            random_state=42,
            n_jobs=-1,
        )

    @property
    def name(self) -> str:
        return "random_forest"
