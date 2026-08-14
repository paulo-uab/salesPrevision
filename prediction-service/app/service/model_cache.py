import hashlib
import pickle
from pathlib import Path
from typing import Optional

from app.config.settings import settings


def _cache_dir() -> Path:
    # Lido a cada chamada (não cacheado a nível de módulo) para que os testes
    # possam apontar settings.MODEL_CACHE_DIR para uma pasta temporária isolada.
    return Path(settings.MODEL_CACHE_DIR)


def _cache_path(pipeline_id: int, target_field: str, group_val: Optional[str], model_name: str) -> Path:
    raw = f"{pipeline_id}:{target_field}:{group_val or ''}:{model_name}"
    key = hashlib.sha256(raw.encode()).hexdigest()
    return _cache_dir() / f"{key}.pkl"


def load_state(pipeline_id: int, target_field: str, group_val: Optional[str], model_name: str) -> Optional[dict]:
    path = _cache_path(pipeline_id, target_field, group_val, model_name)
    if not path.exists():
        return None
    try:
        with path.open("rb") as f:
            return pickle.load(f)
    except Exception:
        # Cache corrompida ou incompatível (ex: mudou de versão da lib) — trata
        # como "sem cache", o chamador reajusta do zero.
        return None


def save_state(pipeline_id: int, target_field: str, group_val: Optional[str], model_name: str, state: dict) -> None:
    path = _cache_path(pipeline_id, target_field, group_val, model_name)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as f:
        pickle.dump(state, f)
