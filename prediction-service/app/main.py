from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.forecast import router as forecast_router
from app.api.models_info import router as models_router
from app.api.pipeline_config import router as config_router
from app.config.database import Base, engine


@asynccontextmanager
async def lifespan(_: FastAPI):
    """Cria as tabelas ao arranque — equivalente a ddl-auto=update do Spring."""
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(
    title="Sales Prediction Service",
    description="Serviço de previsão de vendas — modelos baseline e estatísticos de séries temporais.",
    version="1.0.0",
    lifespan=lifespan,
)

app.include_router(forecast_router)
app.include_router(config_router)
app.include_router(models_router)


@app.get("/health", tags=["health"])
def health() -> dict:
    return {"status": "up"}
