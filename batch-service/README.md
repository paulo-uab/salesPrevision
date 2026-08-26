# batch-service

Runs scheduled Spring Batch jobs that read forecast-ready data from
`ingestion-service`, apply a pipeline fetched from `pipeline-service`, and
POST the result to a prediction API — either the bundled `prediction-service`
or any external, user-configured URL.

**Port:** 8084

## Data model

```
BatchScheduleConfig  (company_id scoped)
  └── BatchExecution (1-to-many)   ← one entry per Spring Batch job run
```

`BatchScheduleConfig` matches `CLAUDE.md`'s description plus two additions:
- `companyId` — multi-tenancy scoping, same pattern as the other services.
- `internalPrediction` (default `false`) — only set `true` when
  `predictionApiUrl` points at this system's own `prediction-service`.
  `ForecastItemWriter` attaches the internal service-account bearer token to
  the outbound POST **only** when this flag is true; it must stay opt-in
  because `predictionApiUrl` is otherwise an arbitrary user-configured URL,
  and the internal token must never be sent to a third party by default.
- `lastRunAt` — persisted (not just in-memory), written by `BatchScheduler`
  right before each cron-triggered run. This is what lets a schedule's "last
  run" survive a service restart — `BatchScheduler.isDue()` computes the
  cron's next fire time *after* `lastRunAt` and compares it to now, instead
  of resetting to an empty map on every restart (which would otherwise let a
  due schedule fire again immediately after every deploy).

`BatchExecution` is unchanged from `CLAUDE.md`.

## Endpoints

| Method | Path | Role required | Description |
|---|---|---|---|
| POST | `/api/batch/schedules` | `BATCH_EDIT` | Create a schedule (pipeline + cron + lookback + prediction API URL). |
| GET | `/api/batch/schedules` | `BATCH_READ` | List the caller's company's schedules. |
| GET | `/api/batch/schedules/{id}` | `BATCH_READ` | Get a schedule by ID. |
| GET | `/api/batch/schedules/{id}/executions` | `BATCH_READ` | Paginated execution history, most recent first. |
| POST | `/api/batch/schedules/{id}/trigger` | `BATCH_EXECUTE` | Manually trigger a run outside its cron schedule; fails if one is already in progress. |

## Role gating & multi-tenancy

`SecurityConfig`: `GET /api/batch/schedules/**` requires `BATCH_READ`,
`POST .../trigger` requires `BATCH_EXECUTE`, `POST /api/batch/schedules`
requires `BATCH_EDIT`. Company isolation is a separate check in
`BatchScheduleService`, same pattern as the other three domain services.

## Spring Batch job

Chunk size 100 (`ForecastJobConfig`): `ForecastItemReader` (pages of 200
`normalizedPayload` records from `ingestion-service`, across all `COMPLETED`
jobs within `lookbackDays`) → `ForecastItemProcessor` (fetches the
`PipelineDto` once per step via `@BeforeStep`, applies `FilterEngine` then
`TransformationEngine`) → `ForecastItemWriter` (chunks of 100, POSTs to
`predictionApiUrl`). The scheduler (`BatchScheduler`) polls every 60s and
checks each active schedule's cron against its persisted `lastRunAt`.

`ForecastItemWriter` also derives the forecast configuration payload
(`date_field`/`target_fields`/`group_field`/`exog_fields`/model/frequency/
horizon/etc.) directly from each `PipelineField`'s `forecastRole`
(`DATE`/`TARGET`/`GROUP`/`EXOG`) — see
[`docs/pipeline-forecast-configuration.md`](../docs/pipeline-forecast-configuration.md)
for what each of those fields means. `FilterEngine`/`TransformationEngine`
behavior (evaluation order, numeric-vs-lexicographic comparison, silent
transformation failures) is unchanged from `CLAUDE.md`.

## Calls to other services

Unlike `ingestion-service`/`pipeline-service`'s calls to `template-service`,
these two calls go **directly** to the target service (not through
`gateway-service`), authenticated as a fixed `internal-service` machine
account (see `InternalServiceAuthConfig`, which logs into `user-service`
directly — `internal.service.auth-url=http://localhost:8086` in dev,
`http://user-service:8086` in `docker`). This account has no `companyId`
claim of its own, so both calls also pass the owning schedule's `companyId`
as an `X-Company-Id` header, which the callee trusts only because the JWT's
subject is already verified as this specific machine account:

- **`pipeline-service`** (`PipelineClient.getPipeline`) — `GET
  /v1/api/pipelines/{id}/dto`. `@Retry`/`@CircuitBreaker` (`pipeline-service`
  instance: 3 attempts, 500ms base wait, exponential backoff ×2); a 404 is
  excluded from both.
- **`ingestion-service`** (`IngestionClient`) — `getCompletedJobs` and
  `getRecordsPage`. `@Retry`/`@CircuitBreaker` (`ingestion-service` instance,
  same retry policy); no 404 carve-out, since neither call has a 404 as a
  normal outcome.

`ForecastItemWriter`'s own `RestClient` (the one that POSTs to
`predictionApiUrl`) deliberately does **not** go through either of `core`'s
auth auto-configurations, since that URL is user-configured and could point
anywhere — see `InternalServiceAuthConfig`'s class comment for why.

## Running standalone

```bash
cd batch-service && ../mvnw spring-boot:run
```

Defaults to the `dev` profile (H2, `jdbc:h2:mem:batchdb`,
`spring.batch.jdbc.initialize-schema=always`, `spring.batch.job.enabled=false`
so jobs never auto-run on startup). `scheduling.enabled=true` toggles the
60-second cron poller without stopping the service. The `docker` profile
repoints the JWKS URI, `services.pipeline.url`, `services.ingestion.url` and
`internal.service.auth-url` at Compose service names.
