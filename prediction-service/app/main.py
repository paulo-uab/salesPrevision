from fastapi import Depends, FastAPI

from app.api.forecast import router as forecast_router
from app.api.models_info import router as models_router
from app.security.auth import verify_token

app = FastAPI(
    title="Sales Prediction Service",
    description=(
        "Sales forecasting service — baseline, statistical and Machine Learning "
        "models over time series. Stateless: the forecast configuration (which "
        "field is the date, the target, the grouping, exogenous variables, "
        "model and parameters) comes inline in every POST /api/forecast request "
        "— see the PipelineConfig schema for the detail of each field. Every "
        "endpoint (except /health) requires a valid Bearer token, issued by "
        "user-service."
    ),
    version="1.0.0",
    openapi_tags=[
        {
            "name": "forecast",
            "description": "Generate forecasts from historical data and an inline configuration.",
        },
        {
            "name": "models",
            "description": "Look up the available models (baseline, statistical, ML) and their characteristics.",
        },
        {
            "name": "health",
            "description": "Service availability check.",
        },
    ],
)

app.include_router(forecast_router, dependencies=[Depends(verify_token)])
app.include_router(models_router, dependencies=[Depends(verify_token)])


@app.get("/health", tags=["health"], summary="Service status")
def health() -> dict:
    return {"status": "up"}
