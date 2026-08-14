from pydantic import BaseModel, Field
from typing import Literal, Optional

ModelType = Literal[
    # baseline
    "naive", "mean", "drift", "seasonal_naive",
    # statistical
    "ses", "holtwinters", "arima",
    # supervised machine learning
    "linear_regression", "random_forest", "xgboost",
]
FrequencyType = Literal["D", "W", "ME", "QE", "YE"]
AggregationType = Literal["sum", "mean", "last", "max", "min"]


class TargetFieldConfig(BaseModel):
    """Configures a target or exogenous field: which field to use and how to
    aggregate multiple records within the same period."""

    field_name: str = Field(
        description="Field name as it appears in the records sent "
                    "(the targetFieldName defined in pipeline-service).",
        examples=["total_amount"],
    )
    aggregation: AggregationType = Field(
        default="sum",
        description="How to aggregate multiple records in the same period: sum, "
                    "mean, last, max or min. E.g. 'sum' for revenue, 'max' for a "
                    "promotion flag (0/1) — if any record in the period had a "
                    "promotion, the whole period counts as promotional.",
    )


class PipelineConfig(BaseModel):
    """Sent inline in every ForecastRequest by batch-service — derived directly
    from the forecastRole/forecastModel/etc. defined in pipeline-service. There
    is no separately persisted configuration here: without this, there would be
    no way to guarantee that the fields referenced here match the names
    pipeline-service actually sends.
    """

    date_field: str = Field(
        description="Field representing the temporal dimension (e.g. 'sale_date'). "
                    "Corresponds to the single field with forecastRole=DATE in the pipeline.",
        examples=["sale_date"],
    )
    target_fields: list[TargetFieldConfig] = Field(
        description="One or more fields to forecast. Corresponds to the fields "
                    "with forecastRole=TARGET in the pipeline — never empty.",
    )
    group_field: Optional[str] = Field(
        default=None,
        description="Optional field to segment the data into independent series "
                    "(e.g. 'product', 'region'). Corresponds to the field with "
                    "forecastRole=GROUP, when present (at most one).",
    )
    exog_fields: list[TargetFieldConfig] = Field(
        default=[],
        description="Exogenous variables (e.g. promotions, price) — never "
                    "forecast, only used as extra input by the models that know "
                    "how to use them (arima and the ML models: linear_regression, "
                    "random_forest, xgboost). Silently ignored by the rest "
                    "(naive/mean/drift/seasonal_naive/ses/holtwinters). There is "
                    "no channel for known future values — the last observed "
                    "value is assumed constant for the whole forecast horizon.",
    )
    forecast_horizon: int = Field(
        default=12, ge=1, le=365,
        description="How many future periods to forecast, in the unit of frequency.",
    )
    model: ModelType = Field(
        default="holtwinters",
        description="Model used for the \"real\" forecast.",
    )
    control_model: ModelType = Field(
        default="naive",
        description="Control/baseline model — always runs in parallel to "
                    "'model' on the same data, to give a "
                    "baseline-vs-chosen-model comparison on every forecast. If "
                    "equal to 'model', it is only computed once.",
    )
    frequency: FrequencyType = Field(
        default="ME",
        description="Temporal granularity: D (daily), W (weekly), ME (monthly), "
                    "QE (quarterly), YE (yearly).",
    )
    season_period: Optional[int] = Field(
        default=None, ge=2,
        description="Seasonal cycle length (e.g. 12 for monthly, 52 for weekly). "
                    "Inferred from frequency when omitted. Only relevant for "
                    "seasonal_naive (needs >= season_period training observations "
                    "just to fit) and holtwinters (needs double that to detect "
                    "seasonality — with less, it degrades to a simple trend).",
    )
    arima_order: list[int] = Field(
        default=[1, 1, 1],
        description="ARIMA (p, d, q) order. Only used when model or "
                    "control_model is 'arima'.",
    )
    sarima_seasonal_order: Optional[list[int]] = Field(
        default=None,
        description="Optional SARIMA seasonal order (P, D, Q, m). Only "
                    "considered when m > 1.",
    )
    n_lags: int = Field(
        default=12, ge=1, le=52,
        description="Number of past values (lags) used as features by the ML "
                    "models. Ignored by the other models.",
    )
    include_date_features: bool = Field(
        default=True,
        description="When True, the ML models receive month/quarter as "
                    "features, plus day-of-week/weekend when frequency='D'.",
    )
    incremental_training: bool = Field(
        default=False,
        description="Only has a real effect for arima and xgboost — reuses the "
                    "previous run's state instead of refitting from scratch. "
                    "Silently ignored for the other models. Does not combine "
                    "with exog_fields: with exogenous variables present, the "
                    "model always fits from scratch.",
    )
