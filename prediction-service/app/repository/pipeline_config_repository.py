from sqlalchemy.orm import Session

from app.entity.pipeline_config import PipelineConfigEntity


class PipelineConfigRepository:
    """Equivalente ao JpaRepository do Spring Data — encapsula todo o acesso à base de dados."""

    def __init__(self, db: Session) -> None:
        self._db = db

    def find_by_pipeline_id(self, pipeline_id: int) -> PipelineConfigEntity | None:
        return (
            self._db.query(PipelineConfigEntity)
            .filter(PipelineConfigEntity.pipeline_id == pipeline_id)
            .first()
        )

    def find_all(self) -> list[PipelineConfigEntity]:
        return self._db.query(PipelineConfigEntity).all()

    def save(self, entity: PipelineConfigEntity) -> PipelineConfigEntity:
        """Cria ou actualiza (upsert por pipeline_id)."""
        existing = self.find_by_pipeline_id(entity.pipeline_id)
        if existing:
            for field in [
                "date_field", "target_fields", "group_field", "forecast_horizon",
                "model", "frequency", "season_period", "arima_order", "sarima_seasonal_order",
            ]:
                setattr(existing, field, getattr(entity, field))
            self._db.commit()
            self._db.refresh(existing)
            return existing
        self._db.add(entity)
        self._db.commit()
        self._db.refresh(entity)
        return entity

    def delete_by_pipeline_id(self, pipeline_id: int) -> bool:
        entity = self.find_by_pipeline_id(pipeline_id)
        if entity is None:
            return False
        self._db.delete(entity)
        self._db.commit()
        return True
