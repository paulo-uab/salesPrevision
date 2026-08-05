from sqlalchemy import Boolean, Integer, JSON, String
from sqlalchemy.orm import Mapped, mapped_column

from app.config.database import Base


class PipelineConfigEntity(Base):
    """Entidade JPA-equivalente para configuração de previsão por pipeline."""

    __tablename__ = "pipeline_config"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    pipeline_id: Mapped[int] = mapped_column(Integer, unique=True, nullable=False, index=True)
    date_field: Mapped[str] = mapped_column(String(120), nullable=False)
    target_fields: Mapped[list] = mapped_column(JSON, nullable=False)
    group_field: Mapped[str | None] = mapped_column(String(120), nullable=True)
    forecast_horizon: Mapped[int] = mapped_column(Integer, default=12)
    model: Mapped[str] = mapped_column(String(50), default="holtwinters")
    frequency: Mapped[str] = mapped_column(String(10), default="ME")
    season_period: Mapped[int | None] = mapped_column(Integer, nullable=True)
    arima_order: Mapped[list | None] = mapped_column(JSON, nullable=True)
    sarima_seasonal_order: Mapped[list | None] = mapped_column(JSON, nullable=True)
    n_lags: Mapped[int] = mapped_column(Integer, default=12)
    include_date_features: Mapped[bool] = mapped_column(Boolean, default=True)
