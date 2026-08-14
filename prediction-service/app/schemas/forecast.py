from pydantic import BaseModel, Field
from typing import Any, Optional

from app.schemas.config import PipelineConfig


class ForecastRequest(BaseModel):
    """Sent by batch-service on every run — historical records plus the
    complete configuration for how to forecast them, all in a single
    request, with no dependency on any prior registration in the
    prediction-service."""

    pipelineId: int = Field(description="Pipeline ID in pipeline-service (for tracing only — not used to look up any configuration here).")
    pipelineName: str
    scheduleConfigId: int
    batchExecutionId: int = Field(description="ID of the batch execution this request belongs to, to associate results back in batch-service.")
    recordCount: int
    records: list[dict[str, Any]] = Field(description="Raw records, already filtered and transformed by pipeline-service — keys are the fields' targetFieldName.")
    config: PipelineConfig = Field(description="How to interpret the records: which field is the date, the target(s), the grouping, exogenous variables, model and parameters.")


class ForecastPoint(BaseModel):
    period: str = Field(description="ISO 8601 timestamp of the forecast period.")
    target_field: str = Field(description="Target field this forecast refers to.")
    group: Optional[str] = Field(default=None, description="Value of group_field, when configured (e.g. product category name).")
    model: str = Field(description="Which model produced this point — 'model' or 'control_model' from the config sent.")
    value: float
    lower_bound: Optional[float] = Field(default=None, description="Lower bound of the 95% confidence interval, when the model produces one (ARIMA only).")
    upper_bound: Optional[float] = Field(default=None, description="Upper bound of the 95% confidence interval, when the model produces one (ARIMA only).")


class ForecastResponse(BaseModel):
    pipeline_id: int
    pipeline_name: str
    batch_execution_id: int
    model_used: str = Field(description="The \"real\" model that was requested (config.model).")
    control_model_used: str = Field(description="The control/baseline model that ran in parallel (config.control_model).")
    forecast_horizon: int
    n_training_series: int = Field(description="Number of series actually fitted — target fields × distinct groups found in the data.")
    predictions: list[ForecastPoint] = Field(description="Forecast points, tagged by model — includes both the \"real\" model and the control one.")
    warnings: list[str] = Field(default=[], description="Non-fatal warnings (e.g. records skipped due to an invalid date, a model that failed for a specific series).")
