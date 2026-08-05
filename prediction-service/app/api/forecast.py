from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.config.database import get_db
from app.exception.exceptions import BadRequestException, ResourceNotFoundException
from app.schemas.forecast import ForecastRequest, ForecastResponse
from app.service.forecast_service import ForecastService

router = APIRouter(prefix="/api/forecast", tags=["forecast"])


@router.post("", response_model=ForecastResponse)
def forecast(
    request: ForecastRequest,
    db: Session = Depends(get_db),
) -> ForecastResponse:
    try:
        return ForecastService(db).run_forecast(request)
    except ResourceNotFoundException as exc:
        raise HTTPException(status_code=404, detail=exc.message)
    except (BadRequestException, ValueError) as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Erro ao executar previsão: {exc}")
