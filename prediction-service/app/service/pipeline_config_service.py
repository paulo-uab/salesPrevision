from sqlalchemy.orm import Session

from app.exception.exceptions import ResourceNotFoundException
from app.mapper.pipeline_config_mapper import PipelineConfigMapper
from app.repository.pipeline_config_repository import PipelineConfigRepository
from app.schemas.config import PipelineConfig


class PipelineConfigService:
    """Lógica de negócio para configurações de previsão por pipeline.

    Equivalente ao BatchScheduleService do batch-service Java:
    orquestra repository + mapper, lança excepções de domínio.
    """

    def __init__(self, db: Session) -> None:
        self._repo = PipelineConfigRepository(db)

    def save(self, config: PipelineConfig) -> PipelineConfig:
        entity = PipelineConfigMapper.to_entity(config)
        saved = self._repo.save(entity)
        return PipelineConfigMapper.to_schema(saved)

    def get_by_pipeline_id(self, pipeline_id: int) -> PipelineConfig:
        entity = self._repo.find_by_pipeline_id(pipeline_id)
        if entity is None:
            raise ResourceNotFoundException(
                f"Configuração não encontrada para pipeline_id={pipeline_id}. "
                "Registe primeiro a configuração em POST /api/config/pipelines."
            )
        return PipelineConfigMapper.to_schema(entity)

    def list_all(self) -> list[PipelineConfig]:
        return [PipelineConfigMapper.to_schema(e) for e in self._repo.find_all()]

    def delete(self, pipeline_id: int) -> None:
        if not self._repo.delete_by_pipeline_id(pipeline_id):
            raise ResourceNotFoundException(
                f"Configuração não encontrada para pipeline_id={pipeline_id}."
            )
