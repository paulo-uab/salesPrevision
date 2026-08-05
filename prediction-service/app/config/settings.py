from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuração centralizada via variáveis de ambiente ou ficheiro .env.

    Equivalente ao application.properties do Spring Boot.
    Exemplo de .env:
        DATABASE_URL=postgresql://user:pass@localhost/predictiondb
        PIPELINE_SERVICE_URL=http://pipeline-service:8083
    """

    DATABASE_URL: str = "sqlite:///./prediction.db"
    PIPELINE_SERVICE_URL: str = "http://localhost:8083"
    APP_PORT: int = 8085

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()
