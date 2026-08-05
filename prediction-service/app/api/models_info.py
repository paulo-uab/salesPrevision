from fastapi import APIRouter

router = APIRouter(prefix="/api/models", tags=["models"])

_MODELS = [
    {
        "name": "naive",
        "type": "baseline",
        "description": "Forecast = último valor observado para todos os horizontes.",
        "min_observations": 1,
        "supports_intervals": False,
    },
    {
        "name": "mean",
        "type": "baseline",
        "description": "Forecast = média de todos os valores históricos.",
        "min_observations": 1,
        "supports_intervals": False,
    },
    {
        "name": "drift",
        "type": "baseline",
        "description": "Extrapolação linear da tendência do primeiro ao último ponto.",
        "min_observations": 2,
        "supports_intervals": False,
    },
    {
        "name": "seasonal_naive",
        "type": "baseline",
        "description": "Forecast = valor do mesmo período no ciclo anterior. Requer season_period ≥ 2.",
        "min_observations": "season_period",
        "supports_intervals": False,
    },
    {
        "name": "ses",
        "type": "statistical",
        "description": "Suavização Exponencial Simples — captura nível sem tendência nem sazonalidade.",
        "min_observations": 2,
        "supports_intervals": False,
    },
    {
        "name": "holtwinters",
        "type": "statistical",
        "description": "Suavização Exponencial Tripla — nível + tendência aditiva + sazonalidade aditiva. "
                       "Degrada para Holt (sem sazonalidade) quando a série é demasiado curta.",
        "min_observations": "2 × season_period",
        "supports_intervals": False,
    },
    {
        "name": "arima",
        "type": "statistical",
        "description": "ARIMA/SARIMA com intervalos de confiança a 95%. "
                       "Configurável via arima_order e sarima_seasonal_order.",
        "min_observations": 10,
        "supports_intervals": True,
    },
    {
        "name": "linear_regression",
        "type": "ml",
        "description": "Regressão Linear com features de lag, rolling statistics e data. "
                       "Interpretável; bom para séries com tendência linear.",
        "min_observations": "n_lags + 1",
        "supports_intervals": False,
        "ml_params": ["n_lags", "include_date_features"],
    },
    {
        "name": "random_forest",
        "type": "ml",
        "description": "Random Forest com features de lag e data. "
                       "Robusto a outliers e não-linearidades; não requer normalização.",
        "min_observations": "n_lags + 1",
        "supports_intervals": False,
        "ml_params": ["n_lags", "include_date_features"],
    },
    {
        "name": "xgboost",
        "type": "ml",
        "description": "XGBoost com features de lag e data. "
                       "Gradient boosting de árvores — excelente para dados tabulares com padrões complexos.",
        "min_observations": "n_lags + 1",
        "supports_intervals": False,
        "ml_params": ["n_lags", "include_date_features"],
    },
]


@router.get("")
def list_models() -> list[dict]:
    return _MODELS
