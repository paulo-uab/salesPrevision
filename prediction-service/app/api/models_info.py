from fastapi import APIRouter

router = APIRouter(prefix="/api/models", tags=["models"])

_MODELS = [
    {
        "name": "naive",
        "type": "baseline",
        "description": "Forecast = last observed value, repeated for every horizon.",
        "min_observations": 1,
        "supports_intervals": False,
    },
    {
        "name": "mean",
        "type": "baseline",
        "description": "Forecast = mean of all historical values.",
        "min_observations": 1,
        "supports_intervals": False,
    },
    {
        "name": "drift",
        "type": "baseline",
        "description": "Linear extrapolation of the trend from the first to the last point.",
        "min_observations": 2,
        "supports_intervals": False,
    },
    {
        "name": "seasonal_naive",
        "type": "baseline",
        "description": "Forecast = value from the same period in the previous cycle. Requires season_period >= 2.",
        "min_observations": "season_period",
        "supports_intervals": False,
    },
    {
        "name": "ses",
        "type": "statistical",
        "description": "Simple Exponential Smoothing — captures level, no trend or seasonality.",
        "min_observations": 2,
        "supports_intervals": False,
    },
    {
        "name": "holtwinters",
        "type": "statistical",
        "description": "Triple Exponential Smoothing — level + additive trend + additive seasonality. "
                       "Degrades to Holt (no seasonality) when the series is too short.",
        "min_observations": "2 × season_period",
        "supports_intervals": False,
    },
    {
        "name": "arima",
        "type": "statistical",
        "description": "ARIMA/SARIMA with 95% confidence intervals. "
                       "Configurable via arima_order and sarima_seasonal_order.",
        "min_observations": 10,
        "supports_intervals": True,
    },
    {
        "name": "linear_regression",
        "type": "ml",
        "description": "Linear Regression with lag, rolling-statistics and date features. "
                       "Interpretable; good for series with a linear trend.",
        "min_observations": "n_lags + 1",
        "supports_intervals": False,
        "ml_params": ["n_lags", "include_date_features"],
    },
    {
        "name": "random_forest",
        "type": "ml",
        "description": "Random Forest with lag and date features. "
                       "Robust to outliers and non-linearities; no normalization required.",
        "min_observations": "n_lags + 1",
        "supports_intervals": False,
        "ml_params": ["n_lags", "include_date_features"],
    },
    {
        "name": "xgboost",
        "type": "ml",
        "description": "XGBoost with lag and date features. "
                       "Gradient-boosted trees — excellent for tabular data with complex patterns.",
        "min_observations": "n_lags + 1",
        "supports_intervals": False,
        "ml_params": ["n_lags", "include_date_features"],
    },
]


@router.get("")
def list_models() -> list[dict]:
    return _MODELS
