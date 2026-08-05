from pydantic import BaseModel, Field
from typing import Literal, Optional

ModelType = Literal[
    # baseline
    "naive", "mean", "drift", "seasonal_naive",
    # estatísticos
    "ses", "holtwinters", "arima",
    # machine learning supervisionado
    "linear_regression", "random_forest", "xgboost",
]
FrequencyType = Literal["D", "W", "ME", "QE", "YE"]
AggregationType = Literal["sum", "mean", "last", "max", "min"]


class TargetFieldConfig(BaseModel):
    """Configura um campo alvo: que campo usar e como agregar múltiplos registos no mesmo período."""
    field_name: str
    aggregation: AggregationType = "sum"


class PipelineConfig(BaseModel):
    pipeline_id: int
    date_field: str                           # campo temporal (ex: "data_venda")
    target_fields: list[TargetFieldConfig]    # ≥ 1 campo a prever (ex: "vendas", "quantidade")
    group_field: Optional[str] = None         # segmentar por este campo (ex: "produto", "regiao")
    forecast_horizon: int = Field(default=12, ge=1, le=365)
    model: ModelType = "holtwinters"
    frequency: FrequencyType = "ME"
    season_period: Optional[int] = Field(default=None, ge=2)
    arima_order: list[int] = Field(default=[1, 1, 1])
    sarima_seasonal_order: Optional[list[int]] = None  # [P, D, Q, m]
    # parâmetros para modelos ML supervisionados
    n_lags: int = Field(default=12, ge=1, le=52)
    include_date_features: bool = True
