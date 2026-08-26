# pipeline-service

Defines *forecast pipelines*: which fields of a template's ingested data to
extract, how to transform them, which records to filter out, which field
plays which role in the forecast (date/target/group/exogenous), and which
model(s) to forecast with — all consumed by `batch-service` on each
scheduled run.

**Port:** 8083

## Data model

```
ForecastPipeline  (company_id + name unique per company)
  ├── PipelineField (1-to-many, cascade all, orphan removal)
  └── PipelineFilter (1-to-many, cascade all, orphan removal)
```

`ForecastPipeline` carries the field/filter lists described in `CLAUDE.md`
plus the forecast-model configuration consumed by `prediction-service`:
`forecastModel`/`controlModel` (plain strings — see the [pipeline forecast
configuration guide](../docs/pipeline-forecast-configuration.md) for the
accepted values and when to use each), `frequency` (default `"ME"`),
`forecastHorizon` (default 12), `seasonPeriod`, `arimaOrder` (CSV of 3
ints, e.g. `"1,1,1"`), `nLags` (default 12), `includeDateFeatures` (default
true), `incrementalTraining` (default false — only meaningful for models
that support it, e.g. `arima`/`xgboost`).

`PipelineField` adds two fields beyond what's in `CLAUDE.md`:
- `forecastRole` (`ForecastFieldRole`: `NONE`/`DATE`/`TARGET`/`GROUP`/`EXOG`,
  default `NONE`) — marks this field's role in the forecast, keyed off its
  *already-transformed* `targetFieldName`. `batch-service` derives
  `date_field`/`target_fields`/`group_field`/`exog_fields` from these roles
  directly, instead of requiring a second, separately-maintained
  configuration.
- `aggregation` — only relevant when `forecastRole=TARGET` or `EXOG`
  (sum/mean/last/max/min); values must match the `AggregationType` the
  Python `prediction-service` expects.

See [`docs/pipeline-forecast-configuration.md`](../docs/pipeline-forecast-configuration.md)
for the full mental model, worked examples and known limitations of the
field-role system — this README doesn't duplicate it.

## Endpoints

| Method | Path | Role required | Description |
|---|---|---|---|
| POST | `v1/api/pipelines` | `PIPELINE_EDIT` | Create a pipeline. Requires exactly one `DATE` field and at least one `TARGET` field, or fails with 400 immediately. |
| GET | `v1/api/pipelines` | `PIPELINE_READ` | List the caller's company's pipelines, optionally filtered by `templateId`. |
| GET | `v1/api/pipelines/{id}` | `PIPELINE_READ` | Get a pipeline by ID. |
| GET | `v1/api/pipelines/{id}/dto` | `PIPELINE_READ` | Internal endpoint — returns the flattened `PipelineDto` that `batch-service` consumes on each run, not meant for end-user browsing. |

## Role gating & multi-tenancy

`SecurityConfig`: `GET /v1/api/pipelines/**` requires `PIPELINE_READ`,
`POST /v1/api/pipelines` requires `PIPELINE_EDIT`. Company isolation is a
separate check in `PipelineService`, on top of the role check — every
pipeline is stamped with and scoped by the caller's JWT `companyId` claim,
same pattern as `template-service`/`ingestion-service`.

## Calls to other services

- `template-service`, to validate `templateId` and read its fields when
  creating a pipeline (`TemplateClient.getTemplate`). Routed **through
  `gateway-service`** (`services.template.url`, `http://localhost:8080` in
  dev / `http://gateway-service:8080` in `docker`), with the caller's
  `Authorization` header relayed rather than a service-account token
  (`services.auth.relay=true`). Wrapped in `@Retry`/`@CircuitBreaker`
  (`template-service` instance); a 404 is excluded from both.

## Running standalone

```bash
cd pipeline-service && ../mvnw spring-boot:run
```

Defaults to the `dev` profile (H2, `jdbc:h2:mem:pipelinedb`). The `docker`
profile repoints `services.template.url` and the JWKS URI at Compose service
names.
