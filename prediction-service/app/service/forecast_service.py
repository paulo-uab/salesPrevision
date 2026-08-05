from sqlalchemy.orm import Session

from app.exception.exceptions import BadRequestException
from app.models.base import BaseForecaster, ForecastResult
from app.models.baseline.drift import DriftForecaster
from app.models.baseline.mean import MeanForecaster
from app.models.baseline.naive import NaiveForecaster
from app.models.baseline.seasonal_naive import SeasonalNaiveForecaster
from app.models.ml.linear_regression import LinearRegressionForecaster
from app.models.ml.random_forest import RandomForestForecaster
from app.models.ml.xgboost_forecaster import XGBoostForecaster
from app.models.statistical.arima import ARIMAForecaster
from app.models.statistical.holtwinters import HoltWintersForecaster
from app.models.statistical.ses import SESForecaster
from app.schemas.config import PipelineConfig
from app.schemas.forecast import ForecastPoint, ForecastRequest, ForecastResponse
from app.service.pipeline_config_service import PipelineConfigService
from app.service.preprocessor import build_series_map, default_season_period


def _build_forecaster(config: PipelineConfig) -> BaseForecaster:
    season = config.season_period or default_season_period(config.frequency)
    match config.model:
        case "naive":
            return NaiveForecaster()
        case "mean":
            return MeanForecaster()
        case "drift":
            return DriftForecaster()
        case "seasonal_naive":
            return SeasonalNaiveForecaster(season_period=season)
        case "ses":
            return SESForecaster()
        case "holtwinters":
            return HoltWintersForecaster(season_period=season)
        case "arima":
            return ARIMAForecaster(
                order=tuple(config.arima_order),
                seasonal_order=tuple(config.sarima_seasonal_order) if config.sarima_seasonal_order else None,
            )
        case "linear_regression":
            return LinearRegressionForecaster(
                n_lags=config.n_lags,
                include_date_features=config.include_date_features,
            )
        case "random_forest":
            return RandomForestForecaster(
                n_lags=config.n_lags,
                include_date_features=config.include_date_features,
            )
        case "xgboost":
            return XGBoostForecaster(
                n_lags=config.n_lags,
                include_date_features=config.include_date_features,
            )
        case _:
            raise BadRequestException(f"Modelo desconhecido: {config.model}")


class ForecastService:
    """Orquestra a execução de previsões.

    Equivalente ao BatchScheduleService do batch-service Java:
    injected via Depends(get_db), delega ao PipelineConfigService e aos modelos.
    """

    def __init__(self, db: Session) -> None:
        self._config_service = PipelineConfigService(db)

    def run_forecast(self, request: ForecastRequest) -> ForecastResponse:
        # ResourceNotFoundException propagada se a configuração não existir → HTTP 404
        config = self._config_service.get_by_pipeline_id(request.pipelineId)

        series_map, warnings = build_series_map(
            records=request.records,
            date_field=config.date_field,
            target_fields=config.target_fields,
            group_field=config.group_field,
            frequency=config.frequency,
        )

        all_predictions: list[ForecastPoint] = []

        for (field_name, group_val), series in series_map.items():
            forecaster = _build_forecaster(config)
            result: ForecastResult = forecaster.fit_predict(series, config.forecast_horizon)

            for i, (ts, val) in enumerate(zip(result.predictions.index, result.predictions.values)):
                lower = round(float(result.lower_bound.iloc[i]), 4) if result.lower_bound is not None else None
                upper = round(float(result.upper_bound.iloc[i]), 4) if result.upper_bound is not None else None
                all_predictions.append(ForecastPoint(
                    period=ts.isoformat(),
                    target_field=field_name,
                    group=group_val,
                    value=round(float(val), 4),
                    lower_bound=lower,
                    upper_bound=upper,
                ))

        all_predictions.sort(key=lambda p: (p.target_field, p.group or "", p.period))

        return ForecastResponse(
            pipeline_id=request.pipelineId,
            pipeline_name=request.pipelineName,
            batch_execution_id=request.batchExecutionId,
            model_used=config.model,
            forecast_horizon=config.forecast_horizon,
            n_training_series=len(series_map),
            predictions=all_predictions,
            warnings=warnings,
        )
