import httpx

from app.config.settings import settings


class PipelineClient:
    """Cliente HTTP para o pipeline-service (:8083).

    Equivalente ao PipelineClient do batch-service Java.
    Usa httpx em vez de RestClient; falhas de conectividade são silenciosas
    (o serviço de previsão não depende do pipeline-service para funcionar).
    """

    def __init__(self) -> None:
        self._base_url = settings.PIPELINE_SERVICE_URL

    def get_pipeline_dto(self, pipeline_id: int) -> dict | None:
        """GET /api/pipelines/{id}/dto — devolve None se o serviço não estiver disponível."""
        try:
            with httpx.Client(timeout=5.0) as client:
                r = client.get(f"{self._base_url}/api/pipelines/{pipeline_id}/dto")
                return r.json() if r.status_code == 200 else None
        except Exception:
            return None
