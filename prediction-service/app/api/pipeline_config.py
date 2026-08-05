from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.exception.exceptions import ResourceNotFoundException
from app.schemas.config import PipelineConfig
from app.service.pipeline_config_service import PipelineConfigService

router = APIRouter(prefix="/api/config/pipelines", tags=["config"])


@router.post("", response_model=PipelineConfig, status_code=201)
def create_or_update(
    config: PipelineConfig,
    db: Session = Depends(get_db),
) -> PipelineConfig:
    return PipelineConfigService(db).save(config)


@router.get("", response_model=list[PipelineConfig])
def list_all(db: Session = Depends(get_db)) -> list[PipelineConfig]:
    return PipelineConfigService(db).list_all()


@router.get("/{pipeline_id}", response_model=PipelineConfig)
def get_one(
    pipeline_id: int,
    db: Session = Depends(get_db),
) -> PipelineConfig:
    try:
        return PipelineConfigService(db).get_by_pipeline_id(pipeline_id)
    except ResourceNotFoundException as exc:
        raise HTTPException(status_code=404, detail=exc.message)


@router.delete("/{pipeline_id}", status_code=204)
def remove(
    pipeline_id: int,
    db: Session = Depends(get_db),
) -> Response:
    try:
        PipelineConfigService(db).delete(pipeline_id)
    except ResourceNotFoundException as exc:
        raise HTTPException(status_code=404, detail=exc.message)
    return Response(status_code=204)
