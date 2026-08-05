from sklearn.linear_model import LinearRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from app.models.ml.base_ml import BaseMLForecaster


class LinearRegressionForecaster(BaseMLForecaster):
    """Regressão Linear com standardização automática das features.

    Bom ponto de partida interpretável: capta tendência linear e
    efeitos aditivos dos lags e da sazonalidade.
    """

    def _create_model(self) -> Pipeline:
        return Pipeline([
            ("scaler", StandardScaler()),
            ("regressor", LinearRegression()),
        ])

    @property
    def name(self) -> str:
        return "linear_regression"
