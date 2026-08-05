from app.entity.pipeline_config import PipelineConfigEntity
from app.schemas.config import PipelineConfig, TargetFieldConfig


class PipelineConfigMapper:
    """Converte entre PipelineConfigEntity (ORM) e PipelineConfig (Pydantic DTO).

    Equivalente ao IngestionEntitiesMapper do ingestion-service Java.
    """

    @staticmethod
    def to_entity(config: PipelineConfig) -> PipelineConfigEntity:
        return PipelineConfigEntity(
            pipeline_id=config.pipeline_id,
            date_field=config.date_field,
            target_fields=[tf.model_dump() for tf in config.target_fields],
            group_field=config.group_field,
            forecast_horizon=config.forecast_horizon,
            model=config.model,
            frequency=config.frequency,
            season_period=config.season_period,
            arima_order=config.arima_order,
            sarima_seasonal_order=config.sarima_seasonal_order,
            n_lags=config.n_lags,
            include_date_features=config.include_date_features,
        )

    @staticmethod
    def to_schema(entity: PipelineConfigEntity) -> PipelineConfig:
        return PipelineConfig(
            pipeline_id=entity.pipeline_id,
            date_field=entity.date_field,
            target_fields=[TargetFieldConfig(**tf) for tf in (entity.target_fields or [])],
            group_field=entity.group_field,
            forecast_horizon=entity.forecast_horizon,
            model=entity.model,
            frequency=entity.frequency,
            season_period=entity.season_period,
            arima_order=entity.arima_order or [1, 1, 1],
            sarima_seasonal_order=entity.sarima_seasonal_order,
            n_lags=entity.n_lags if entity.n_lags is not None else 12,
            include_date_features=entity.include_date_features if entity.include_date_features is not None else True,
        )
