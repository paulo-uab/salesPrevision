import pytest
from fastapi.testclient import TestClient

from app.config.settings import settings
from app.main import app
from app.security.auth import verify_token

_FAKE_CLAIMS = {"sub": "test-user", "roles": [], "companyId": 1}


@pytest.fixture
def client(tmp_path, monkeypatch):
    """Cliente de teste com autenticação simulada — o prediction-service não
    tem base de dados (a configuração de previsão vem inline em cada pedido).
    Isola a cache de modelos incrementais numa pasta temporária, para nunca
    escrever ficheiros reais no repositório durante os testes."""
    monkeypatch.setattr(settings, "MODEL_CACHE_DIR", str(tmp_path / "model_cache"))
    app.dependency_overrides[verify_token] = lambda: _FAKE_CLAIMS
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
