# prediction-service

Python/FastAPI forecasting service. Stateless: every `/api/forecast` request
carries both the historical records and the complete forecast configuration
(which field is the date, the target(s), the grouping, exogenous variables,
model choice and its parameters) inline — there's no database and no
persisted pipeline/schedule state on this side. The only thing written to
disk is a small on-disk cache used for models that support incremental
training.

**Port:** 8085

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/forecast` | Bearer JWT | Runs a forecast. Takes historical records + the full `PipelineConfig` and returns forecasts for both the chosen model and the `control_model`, always computed in parallel (so there's a baseline-vs-chosen-model comparison on every call). When `incremental_training=true`, the fitted model's state is saved to `MODEL_CACHE_DIR` (see `app/service/model_cache.py`) and reused on the next call for the same pipeline/field/group/model. |
| GET | `/api/models` | Bearer JWT | Lists the available models (baseline/statistical/ML) with their descriptions, minimum observation requirements, whether they support confidence intervals, and any extra ML params. |
| GET | `/health` | public | Service status (`{"status": "up"}`). |

Note: `gateway-service` also has a route for `/api/config/**` pointed at this
service (`prediction-config`, circuit breaker `prediction-service`), but no
corresponding `/api/config` router currently exists in `app/api/` —
`app/main.py` only mounts `forecast_router` and `models_router`.

### Available models

Baseline: `naive`, `mean`, `drift`, `seasonal_naive`. Statistical: `ses`,
`holtwinters`, `arima` (the only one with confidence intervals and `EXOG`
support). ML: `linear_regression`, `random_forest`, `xgboost` (also supports
`EXOG` and incremental training). See
[`docs/pipeline-forecast-configuration.md`](../docs/pipeline-forecast-configuration.md)
for when to use each and the full field-role (`DATE`/`TARGET`/`GROUP`/`EXOG`)
mental model that `pipeline-service`/`batch-service` use to build this
service's inline configuration.

## Authorization

Every endpoint except `/health` requires a valid Bearer JWT
(`app/security/auth.py`, `verify_token` dependency). Unlike the Java
services, there's no FastAPI equivalent of Spring's
`oauth2ResourceServer` auto-configuration, so JWKS validation is done by
hand: `PyJWKClient` fetches and caches `user-service`'s public key from
`JWKS_URL`, and the token's RS256 signature is verified against it. No
`ServiceRole` is checked — any valid token issued by `user-service` is
accepted; there is no forecasting-specific role in `ServiceRole`.

## Calls to other services

- `user-service`, to fetch the JWKS document used to verify inbound tokens
  (`JWKS_URL`, default `http://localhost:8086/.well-known/jwks.json`, set to
  `http://user-service:8086/.well-known/jwks.json` in `docker-compose.yml`).

It never calls `pipeline-service`, `ingestion-service` or `batch-service` —
those push data to it (via `batch-service`'s `ForecastItemWriter`), it never
pulls.

## Running standalone

```bash
cd prediction-service
python -m venv .venv && source .venv/bin/activate   # or .venv\Scripts\activate on Windows
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8085
```

Interactive API docs are available at `/docs` once running. Configuration is
via environment variables or a `.env` file (`app/config/settings.py`):
`APP_PORT` (default 8085), `JWKS_URL`, `MODEL_CACHE_DIR` (default
`./model_cache`). Requires Python 3.12 and the packages in
`requirements.txt` (FastAPI, uvicorn, pandas, numpy, statsmodels,
scikit-learn, xgboost, PyJWT, pytest for the test suite under `tests/`).

The provided `Dockerfile` builds a `python:3.12-slim` image and runs the same
`uvicorn` command on port 8085 — this is the image `docker-compose.yml`
builds via `build: context: ./prediction-service` (its own directory, unlike
the Java services which build from the repo root).
