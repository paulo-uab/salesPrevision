import jwt
from fastapi import Header, HTTPException
from jwt import PyJWKClient

from app.config.settings import settings

# Cacheia as chaves públicas — equivalente ao jwk-set-uri dos outros serviços,
# aqui replicado manualmente porque o FastAPI não tem oauth2ResourceServer pronto.
_jwk_client = PyJWKClient(settings.JWKS_URL)


def verify_token(authorization: str = Header(None)) -> dict:
    """Valida o Bearer token contra o JWKS do user-service (assinatura RS256 + validade).

    Não impõe roles específicas — não existe nenhum ServiceRole dedicado à
    previsão; qualquer token válido emitido pelo user-service é aceite.
    """
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Token de autenticação em falta.")

    token = authorization.removeprefix("Bearer ").strip()
    try:
        signing_key = _jwk_client.get_signing_key_from_jwt(token)
        claims = jwt.decode(token, signing_key.key, algorithms=["RS256"])
    except jwt.PyJWTError as exc:
        raise HTTPException(status_code=401, detail=f"Token inválido: {exc}")

    return claims
