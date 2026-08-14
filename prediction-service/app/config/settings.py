from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Configuração centralizada via variáveis de ambiente ou ficheiro .env.

    Equivalente ao application.properties do Spring Boot. Sem base de dados —
    a configuração de previsão vem inline em cada ForecastRequest.
    Exemplo de .env:
        JWKS_URL=http://localhost:8086/.well-known/jwks.json
        MODEL_CACHE_DIR=./model_cache
    """

    APP_PORT: int = 8085
    JWKS_URL: str = "http://localhost:8086/.well-known/jwks.json"
    # Estado de modelos com treino incremental (arima, xgboost) fica em disco,
    # em vez de reintroduzir uma base de dados só para isto.
    MODEL_CACHE_DIR: str = "./model_cache"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")


settings = Settings()
