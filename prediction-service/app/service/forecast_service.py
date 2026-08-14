from typing import Optional

import pandas as pd

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
from app.service import model_cache
from app.service.preprocessor import build_series_map, default_season_period


def _build_forecaster(config: PipelineConfig, model_name: str) -> BaseForecaster:
    season = config.season_period or default_season_period(config.frequency)
    match model_name:
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
                frequency=config.frequency,
            )
        case "random_forest":
            return RandomForestForecaster(
                n_lags=config.n_lags,
                include_date_features=config.include_date_features,
                frequency=config.frequency,
            )
        case "xgboost":
            return XGBoostForecaster(
                n_lags=config.n_lags,
                include_date_features=config.include_date_features,
                frequency=config.frequency,
            )
        case _:
            raise BadRequestException(f"Modelo desconhecido: {model_name}")


class ForecastService:
    """Orquestra a execução de previsões — sem estado, sem base de dados: toda a
    configuração vem inline no ForecastRequest (ver app/schemas/config.py)."""

    def run_forecast(self, request: ForecastRequest) -> ForecastResponse:
        config = request.config

        series_map, exog_series_map, warnings = build_series_map(
            records=request.records,
            date_field=config.date_field,
            target_fields=config.target_fields,
            group_field=config.group_field,
            frequency=config.frequency,
            exog_fields=config.exog_fields,
        )
        exog_field_names = [f.field_name for f in config.exog_fields]

        all_predictions: list[ForecastPoint] = []
        # Modelo de controlo corre sempre a par do modelo escolhido — se forem
        # o mesmo, só computamos uma vez.
        models_to_run = {config.model, config.control_model}

        for (field_name, group_val), series in series_map.items():
            label = f"'{field_name}'" + (f", grupo '{group_val}'" if group_val else "")
            exog_frame = self._build_exog_frame(exog_series_map, group_val, exog_field_names)

            for model_name in models_to_run:
                forecaster = _build_forecaster(config, model_name)
                try:
                    if exog_frame is not None and forecaster.supports_exog:
                        # exog e incremental_training não se combinam nesta versão —
                        # havendo exógenas, ajusta-se sempre do zero.
                        forecaster.fit_with_exog(series, exog_frame)
                        result = forecaster.predict_with_exog(config.forecast_horizon)
                    elif config.incremental_training and forecaster.supports_incremental:
                        result = self._fit_incremental(
                            forecaster, series, config, request.pipelineId, field_name, group_val, model_name,
                        )
                    else:
                        result = forecaster.fit_predict(series, config.forecast_horizon)
                except ValueError as exc:
                    warnings.append(f"Modelo '{model_name}' falhou para {label}: {exc}")
                    continue

                for i, (ts, val) in enumerate(zip(result.predictions.index, result.predictions.values)):
                    lower = round(float(result.lower_bound.iloc[i]), 4) if result.lower_bound is not None else None
                    upper = round(float(result.upper_bound.iloc[i]), 4) if result.upper_bound is not None else None
                    all_predictions.append(ForecastPoint(
                        period=ts.isoformat(),
                        target_field=field_name,
                        group=group_val,
                        model=model_name,
                        value=round(float(val), 4),
                        lower_bound=lower,
                        upper_bound=upper,
                    ))

        all_predictions.sort(key=lambda p: (p.target_field, p.group or "", p.model, p.period))

        return ForecastResponse(
            pipeline_id=request.pipelineId,
            pipeline_name=request.pipelineName,
            batch_execution_id=request.batchExecutionId,
            model_used=config.model,
            control_model_used=config.control_model,
            forecast_horizon=config.forecast_horizon,
            n_training_series=len(series_map),
            predictions=all_predictions,
            warnings=warnings,
        )

    @staticmethod
    def _build_exog_frame(
        exog_series_map: dict, group_val: Optional[str], exog_field_names: list[str],
    ) -> Optional[pd.DataFrame]:
        if not exog_field_names:
            return None
        columns = {
            name: exog_series_map[(name, group_val)]
            for name in exog_field_names
            if (name, group_val) in exog_series_map
        }
        return pd.DataFrame(columns) if columns else None

    def _fit_incremental(
        self,
        forecaster: BaseForecaster,
        series: pd.Series,
        config: PipelineConfig,
        pipeline_id: int,
        field_name: str,
        group_val: Optional[str],
        model_name: str,
    ) -> ForecastResult:
        cached = model_cache.load_state(pipeline_id, field_name, group_val, model_name)

        if cached is not None:
            try:
                forecaster.set_state(cached["forecaster_state"], cached["last_date"], series.index.freq)
                new_points = series[series.index > cached["last_date"]]
                result = forecaster.update(new_points, config.forecast_horizon)
            except Exception:
                # Cache incompatível (ex: n_lags/arima_order mudaram desde a
                # última execução) — trata como se não houvesse cache.
                result = forecaster.fit_predict(series, config.forecast_horizon)
        else:
            result = forecaster.fit_predict(series, config.forecast_horizon)

        model_cache.save_state(pipeline_id, field_name, group_val, model_name, {
            "last_date": series.index[-1],
            "forecaster_state": forecaster.get_state(),
        })
        return result
