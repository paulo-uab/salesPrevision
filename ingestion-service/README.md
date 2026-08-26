# ingestion-service

Accepts file uploads, validates each record against a template fetched from
`template-service`, and stores the raw + normalized results. Processing is
asynchronous: uploading a file publishes a Kafka event, and a consumer in
this same service does the actual validation work off the request thread.

**Port:** 8082

## Data model

```
IngestionJob  (company_id scoped)
  ├── IngestedRecord (1-to-many)   ← one row per source line, VALID/INVALID/WARNING
  └── IngestionError (1-to-many)   ← one entry per failed validation rule
```

Entities match `CLAUDE.md`'s description, with one addition: `IngestionJob`
now carries a `companyId` column (multi-tenancy — see below).

## Endpoints

| Method | Path | Role required | Description |
|---|---|---|---|
| POST | `v1/api/ingestion/jobs` | `INGESTION_EDIT` | Upload a file + `templateId`. Creates the job (status `RECEIVED`) and, by default (`autoProcess=true`), publishes it to Kafka for async processing. |
| POST | `v1/api/ingestion/jobs/{jobId}/process` | `INGESTION_EXECUTE` | (Re)submit an existing job for processing — also just publishes to Kafka. |
| GET | `v1/api/ingestion/jobs` | `INGESTION_READ` | List jobs for the caller's company, filterable by `templateId`/`status`. |
| GET | `v1/api/ingestion/jobs/{jobId}` | `INGESTION_READ` | Get a job by ID. |
| GET | `v1/api/ingestion/jobs/{jobId}/records` | `INGESTION_READ` | Paginated records. |
| GET | `v1/api/ingestion/jobs/{jobId}/errors` | `INGESTION_READ` | Paginated errors. |

## Async processing via Kafka

This is a divergence from the originally-synchronous design described
elsewhere: `createJob`/`submitForProcessing` no longer call the processor
inline. Instead:

1. `IngestionEventProducer` publishes an `IngestionJobEvent(jobId, correlationId)`
   to the `ingestion.jobs` topic.
2. `IngestionEventConsumer`, annotated `@RetryableTopic(attempts = "3", ...)`,
   consumes it and calls `IngestionService.processJob(jobId)` — the actual
   CSV parsing/validation/normalization logic is unchanged from `CLAUDE.md`'s
   description (processor-per-`FileType`, CSV implemented, others not yet).
3. On failure, Spring Kafka's retry topic pattern retries up to 3 times
   (suffixed retry topics), and after exhausting retries routes the event to
   a dead-letter topic (`ingestion.jobs.dlt`). `@DltHandler` marks the job
   `FAILED` with a Portuguese error message instead of leaving it stuck in
   `RECEIVED`/`PROCESSING` forever.

The correlation ID from the original HTTP request is carried through the
Kafka message and restored into the log MDC on both the retry and DLT paths,
so a job's whole async lifecycle stays traceable back to the upload request.

## Role gating & multi-tenancy

`SecurityConfig`: `GET .../jobs/**` requires `INGESTION_READ`,
`POST .../jobs/*/process` requires `INGESTION_EXECUTE`, `POST .../jobs`
requires `INGESTION_EDIT`. Company isolation is layered on top in
`IngestionService`: every job is stamped with the caller's JWT `companyId`
claim, and every list/lookup is scoped to it. One exception:
`batch-service`'s direct, cron-triggered calls authenticate as a fixed
`internal-service` account with no company context of its own — for that
one subject, `companyId` is instead trusted from an `X-Company-Id` header
(only because the JWT itself is already cryptographically verified as that
specific machine account).

## Calls to other services

- `template-service`, to fetch the template a job validates against
  (`TemplateClient.getTemplate`). Routed **through `gateway-service`**
  (`services.template.url=http://localhost:8080` in dev,
  `http://gateway-service:8080` in the `docker` profile), with the caller's
  own `Authorization` header relayed onto the call (`services.auth.relay=true`,
  `core.tokenrelay`) rather than a separate service-account token — so
  `template-service` sees the original caller's `companyId`.
  Wrapped in `@Retry`/`@CircuitBreaker` (`template-service` instance); a 404
  is excluded from both (a legitimate "not found", not a transient failure).

## Running standalone

```bash
cd ingestion-service && ../mvnw spring-boot:run
```

Requires a reachable Kafka broker (`spring.kafka.bootstrap-servers`,
default `localhost:9092`) — see the `kafka` service in the repo root
`docker-compose.yml`, or point at your own. Defaults to the `dev` profile
(H2, `jdbc:h2:mem:ingestiondb`); the `docker` profile repoints
`services.template.url`, the JWKS URI, and the Kafka bootstrap servers at
Compose service names. File upload limit is 20MB
(`spring.servlet.multipart.max-file-size/max-request-size`); files are
stored under `uploads/template_{id}/` (`app.storage.upload-dir`).
