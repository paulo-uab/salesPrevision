from fastapi import APIRouter, HTTPException

from app.exception.exceptions import BadRequestException
from app.schemas.forecast import ForecastRequest, ForecastResponse
from app.service.forecast_service import ForecastService

router = APIRouter(prefix="/api/forecast", tags=["forecast"])


@router.post(
    "",
    response_model=ForecastResponse,
    summary="Generate a forecast",
    description=(
        "Takes historical records + the complete forecast configuration (see "
        "PipelineConfig) and returns future forecasts for both the chosen model "
        "and the control model, always computed in parallel. No state is "
        "persisted between calls — each request is self-sufficient, except "
        "when incremental_training=true, in which case the fitted model's "
        "state is saved to a local cache (see app/service/model_cache.py) for "
        "the next call with the same pipelineId/field/group/model."
    ),
)
def forecast(request: ForecastRequest) -> ForecastResponse:
    try:
        return ForecastService().run_forecast(request)
    except (BadRequestException, ValueError) as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Erro ao executar previsão: {exc}")
