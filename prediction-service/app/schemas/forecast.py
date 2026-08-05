from pydantic import BaseModel
from typing import Any, Optional


class ForecastRequest(BaseModel):
    pipelineId: int
    pipelineName: str
    scheduleConfigId: int
    batchExecutionId: int
    recordCount: int
    records: list[dict[str, Any]]


class ForecastPoint(BaseModel):
    period: str
    target_field: str              # campo alvo a que se refere esta previsão
    group: Optional[str] = None   # valor do group_field, se configurado
    value: float
    lower_bound: Optional[float] = None
    upper_bound: Optional[float] = None


class ForecastResponse(BaseModel):
    pipeline_id: int
    pipeline_name: str
    batch_execution_id: int
    model_used: str
    forecast_horizon: int
    n_training_series: int         # nº de séries ajustadas (campos × grupos)
    predictions: list[ForecastPoint]
    warnings: list[str] = []
