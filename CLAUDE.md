# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules (run from repo root)
./mvnw clean package

# Run a specific service (run from its directory)
cd template-service  && ../mvnw spring-boot:run
cd ingestion-service && ../mvnw spring-boot:run
cd pipeline-service  && ../mvnw spring-boot:run
cd batch-service     && ../mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Clean build artifacts
./mvnw clean
```

H2 console available at `/h2-console` on each service when running locally. File upload limit is 20MB.

---

## System overview

Multi-module Maven project. Spring Boot 4.0 / Java 17. Four microservices plus a shared `core` library. The system ingests tabular data files, validates them against configurable templates, lets users define selection/transformation pipelines over that data, and feeds the result to an external prediction API on a schedule.

```
core  (shared DTOs, enums, exceptions — not a runnable service)
  ↑ depended on by all four services below

template-service  :8081   Define templates that describe the shape of incoming files
ingestion-service :8082   Upload files, validate and store records against a template
pipeline-service  :8083   Define which fields/filters/transformations to apply to ingested data
batch-service     :8084   Run scheduled Spring Batch jobs that extract → transform → send to prediction API
```

### Inter-service calls

```
batch-service ──GET /api/pipelines/{id}/dto──────────────► pipeline-service
batch-service ──GET /api/ingestion/jobs?templateId&status── ingestion-service
batch-service ──GET /api/ingestion/jobs/{id}/records──────► ingestion-service
batch-service ──POST {predictionApiUrl}───────────────────► external prediction API (user-configured)

ingestion-service ──GET v1/api/templates/{id}─────────────► template-service
pipeline-service  ──GET v1/api/templates/{id}─────────────► template-service
```

---

## Module: core

Shared library. No web server, no main class (well, a stub `CoreApplication` exists but is not deployed).

### Packages

| Package | Contents |
|---|---|
| `dto/ingestion/` | `TemplateDto`, `IngestionJobResponse`, `IngestedRecordResponse`, `IngestionErrorResponse`, `CreateIngestionJobResponse` |
| `dto/template/` | `CreateIngestionTemplateRequest`, `IngestionTemplateResponse`, `TemplateFieldRequest/Response`, `TemplateValidationRuleRequest/Response` |
| `dto/pipeline/` | `PipelineDto` (consumed by batch-service to deserialise pipeline-service responses) |
| `enums/` | `FileType`, `IngestionStatus`, `ValidationStatus`, `FieldDataType`, `TemplateRuleType`, `TransformationType`, `FilterOperator`, `LogicalOperator`, `BatchExecutionStatus` |
| `exception/` | `BadRequestException` (400), `ResourceNotFoundException` (404), `GlobalExceptionHandler` → `{status, error, message, details}` |

---

## Module: template-service (:8081)

Manages ingestion templates. Templates describe the expected shape of a file (columns, types, validation rules).

### Data model

```
IngestionTemplate
  └── TemplateField (1-to-many, cascade all, orphan removal)
        └── TemplateValidationRule (1-to-many, cascade all, orphan removal)
```

#### IngestionTemplate
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `name` | String(150) unique | |
| `description` | String(1000) | |
| `fileType` | FileType enum | CSV, JSON, XLSX, TXT, UNKNOWN |
| `delimiter` | String(10) | CSV only |
| `hasHeader` | Boolean | default true |
| `sheetName` | String(150) | XLSX only |
| `rootPath` | String(255) | JSON root path |
| `version` | Integer | default 1 |
| `active` | Boolean | default true |
| `createdAt`, `updatedAt` | LocalDateTime | |

#### TemplateField
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `template` | ManyToOne | |
| `fieldName` | String(120) | canonical name in the system |
| `sourceName` | String(120) | column header or key in the source file |
| `dataType` | FieldDataType enum | STRING, INTEGER, LONG, DECIMAL, BOOLEAN, DATE, DATETIME |
| `required` | Boolean | |
| `positionIndex` | Integer | fallback for headerless files |
| `dateFormat` | String(50) | |
| `defaultValue` | String(255) | |
| `validationRegex` | String(500) | |
| `targetPath` | String(255) | |
| `active` | Boolean | |

#### TemplateValidationRule
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `field` | ManyToOne | |
| `ruleType` | TemplateRuleType enum | NOT_NULL, REGEX, MIN, MAX, ENUM, DATE_FORMAT |
| `ruleValue` | String(500) | e.g. regex pattern, min value, comma-separated ENUM values |
| `message` | String(500) | custom error message (falls back to Portuguese default) |
| `active` | Boolean | |

### Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `v1/api/templates` | Create template |
| GET | `v1/api/templates` | List all templates |
| GET | `v1/api/templates/{id}` | Get template by id |

### Notes
- Spring Security is a dependency but unauthenticated locally.
- H2 database: `jdbc:h2:mem:templatedb`.

---

## Module: ingestion-service (:8082)

Accepts file uploads, validates records against a template, and stores the results.

### Data model

```
IngestionJob
  ├── IngestedRecord (1-to-many)   ← one row per source line, VALID/INVALID/WARNING
  └── IngestionError (1-to-many)   ← one entry per failed validation rule
```

#### IngestionJob
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `templateId` | Long | FK reference to template-service (not a JPA relation) |
| `templateName` | String(150) | denormalised |
| `status` | IngestionStatus | RECEIVED → PROCESSING → COMPLETED / FAILED |
| `originalFileName` | String(255) | |
| `storedFileName` | String(255) | `{UUID}_{sanitised original}` |
| `contentType` | String(100) | |
| `fileSize` | Long | bytes |
| `storagePath` | String(500) | absolute FS path under `uploads/template_{id}/` |
| `checksum` | String(128) | SHA-256 hex |
| `startedAt`, `finishedAt` | LocalDateTime | |
| `createdAt` | LocalDateTime | |
| `createdBy` | String(100) | |
| `errorMessage` | String(4000) | set when status = FAILED |
| `recordCount`, `errorCount` | Long | |

#### IngestedRecord
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `ingestionJob` | ManyToOne | |
| `recordIndex` | Long | 1-based row number |
| `rawPayload` | TEXT | JSON map of original values |
| `normalizedPayload` | TEXT | JSON map after type conversion |
| `validationStatus` | ValidationStatus | VALID, INVALID, WARNING |
| `createdAt` | LocalDateTime | |

#### IngestionError
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `ingestionJob` | ManyToOne | |
| `record` | ManyToOne (optional) | |
| `fieldName` | String(100) | |
| `errorType` | String(100) | VALIDATION, REGEX, TYPE_CONVERSION, NOT_NULL, MIN, MAX, ENUM, DATE_FORMAT |
| `message` | String(4000) | Portuguese message |
| `rawValue` | String(1000) | original string from the file |
| `lineNumber` | Long | |
| `createdAt` | LocalDateTime | |

### Request flow

```
POST /api/ingestion/jobs (multipart file + templateId)
  → IngestionService.createJob()
      → templateClient.getTemplate(templateId)      ← calls template-service
      → detect file type from extension
      → store file: uploads/template_{id}/{UUID}_{name}
      → compute SHA-256 checksum
      → save IngestionJob (status=RECEIVED)
      → if autoProcess=true → processJob()
            → IngestionProcessorFactory.getProcessor(FileType)
            → AbstractIngestionProcessor.process(job, template)
                → doProcess() — implemented per format
                    → for each row: convertAndValidateField() per TemplateField
                    → saveRecord() + saveError() per validation failure
            → update job status → COMPLETED / FAILED
```

### Processor strategy

| FileType | Processor | Status |
|---|---|---|
| CSV | `CsvIngestionProcessor` | Implemented |
| JSON | — | Not yet implemented |
| XLSX | — | Not yet implemented |
| TXT | — | Not yet implemented |

**Adding a new format:** implement `IngestionProcessor` (or extend `AbstractIngestionProcessor`), annotate with `@Service`. `IngestionProcessorFactory` auto-discovers all beans.

### CSV specifics
- Delimiter: comma by default; `\t` string resolves to tab; any other value used literally.
- Parsing: `String.split()` — no quoted-field or escaped-delimiter support.
- Field resolution: by `sourceName` against header first, then `positionIndex` fallback.

### Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/ingestion/jobs` | Upload file, create (and optionally auto-process) job |
| POST | `/api/ingestion/jobs/{jobId}/process` | Process an existing job |
| GET | `/api/ingestion/jobs` | List jobs (`templateId`, `status` filters) |
| GET | `/api/ingestion/jobs/{jobId}` | Get job details |
| GET | `/api/ingestion/jobs/{jobId}/records` | Paginated records |
| GET | `/api/ingestion/jobs/{jobId}/errors` | Paginated errors |

### Storage & security
- Files stored under `uploads/` (configurable via `app.storage.upload-dir`).
- H2 database: `jdbc:h2:mem:ingestiondb`.
- `ObjectMapper` bean declared explicitly in `JacksonConfig` (Spring Boot 4 `spring-boot-starter-webmvc` does not auto-configure it).

---

## Module: pipeline-service (:8083)

Defines *forecast pipelines*: which fields of a template's ingested data should be extracted, how they should be transformed, and which records should be filtered out before sending to the prediction API.

### Data model

```
ForecastPipeline
  ├── PipelineField (1-to-many, cascade all, orphan removal)
  └── PipelineFilter (1-to-many, cascade all, orphan removal)
```

#### ForecastPipeline
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `templateId` | Long | reference to template-service |
| `templateName` | String(150) | denormalised |
| `name` | String(150) unique | |
| `description` | String(1000) | |
| `active` | Boolean | |
| `createdAt`, `updatedAt` | LocalDateTime | |

#### PipelineField — field selection and transformation
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `pipeline` | ManyToOne | |
| `sourceFieldName` | String(120) | field name as it appears in `normalizedPayload` |
| `targetFieldName` | String(120) | key name in the prediction API payload |
| `transformationType` | TransformationType enum | NONE, RENAME, SCALE, DATE_FORMAT, CAST, REPLACE |
| `transformationConfig` | TEXT | JSON params; see transformation details below |
| `positionIndex` | Integer | output ordering |
| `active` | Boolean | |

**Transformation configs (JSON):**
- `SCALE` → `{"factor": 0.01}`
- `DATE_FORMAT` → `{"from": "dd/MM/yyyy", "to": "yyyy-MM-dd"}`
- `REPLACE` → `{"regex": "\\s+", "replacement": "_"}`
- `NONE` / `RENAME` / `CAST` → no config needed

#### PipelineFilter — record-level filtering
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `pipeline` | ManyToOne | |
| `fieldName` | String(120) | field in `normalizedPayload` to evaluate |
| `operator` | FilterOperator enum | EQ, NEQ, GT, GTE, LT, LTE, IN, NOT_IN, IS_NULL, IS_NOT_NULL, BETWEEN, CONTAINS |
| `value` | String(500) | comparison value; `IN`/`NOT_IN` use comma-separated list; `BETWEEN` uses `"min,max"` |
| `logicalOperator` | LogicalOperator enum | AND / OR — how this filter combines with the *next* filter |
| `orderIndex` | Integer | evaluation order |

### Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/pipelines` | Create pipeline |
| GET | `/api/pipelines` | List all (`templateId` filter optional) |
| GET | `/api/pipelines/{id}` | Get pipeline |
| GET | `/api/pipelines/{id}/dto` | Get as `PipelineDto` — called by batch-service |

### Notes
- `ObjectMapper` bean declared explicitly in `JacksonConfig`.
- H2 database: `jdbc:h2:mem:pipelinedb`.

---

## Module: batch-service (:8084)

Runs scheduled Spring Batch jobs. Each schedule ties a pipeline to a cron expression, a lookback window, and a prediction API URL.

### Data model

```
BatchScheduleConfig
  └── BatchExecution (1-to-many)   ← one entry per Spring Batch job run
```

#### BatchScheduleConfig
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `pipelineId` | Long | reference to pipeline-service |
| `pipelineName` | String(150) | denormalised |
| `cronExpression` | String(150) | standard 6-part Spring cron |
| `lookbackDays` | Integer | default 7; only jobs created within this window are read |
| `predictionApiUrl` | String(500) | full URL of the external prediction API |
| `active` | Boolean | inactive schedules are skipped by the scheduler |
| `createdBy` | String(100) | |
| `createdAt`, `updatedAt` | LocalDateTime | |

#### BatchExecution
| Field | Type | Notes |
|---|---|---|
| `id` | Long PK | |
| `scheduleConfig` | ManyToOne | |
| `springBatchJobExecutionId` | Long | cross-reference to Spring Batch's own tables |
| `status` | BatchExecutionStatus | RUNNING, COMPLETED, FAILED, SKIPPED |
| `startedAt`, `finishedAt` | LocalDateTime | |
| `recordsSent`, `recordsFailed` | Long | |
| `errorMessage` | String(4000) | |
| `createdAt` | LocalDateTime | |

### Spring Batch job flow

```
BatchScheduler (polls every 60s, checks cron with CronExpression.next())
  → BatchScheduleService.runJob(config)
      → save BatchExecution (status=RUNNING)
      → JobLauncher.run(forecastJob, {scheduleConfigId, batchExecutionId, startedAt})
            → ForecastItemReader   reads normalizedPayload records from ingestion-service
                                   (pages of 200, all COMPLETED jobs within lookbackDays)
            → ForecastItemProcessor  fetches PipelineDto from pipeline-service once (@BeforeStep)
                                     applies FilterEngine  → returns null to skip record
                                     applies TransformationEngine → returns transformed Map
            → ForecastItemWriter   chunks of 100, POSTs to predictionApiUrl
                                   payload: {pipelineId, pipelineName, scheduleConfigId,
                                             batchExecutionId, recordCount, records:[...]}
      → update BatchExecution (status=COMPLETED/FAILED)
```

### FilterEngine logic

Filters are evaluated in `orderIndex` order. Each filter's `logicalOperator` connects it to the **next** filter (i.e., filter[0].logicalOperator determines how filter[0] and filter[1] combine). Numeric comparisons (GT/GTE/LT/LTE/BETWEEN) use `BigDecimal`; fall back to lexicographic for non-numeric values.

### TransformationEngine config parsing

`transformationConfig` is a JSON string parsed at runtime. Failures in transformation are silent — the original value is returned unchanged.

### Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/batch/schedules` | Create schedule |
| GET | `/api/batch/schedules` | List all schedules |
| GET | `/api/batch/schedules/{id}` | Get schedule |
| GET | `/api/batch/schedules/{id}/executions` | Paginated execution history |
| POST | `/api/batch/schedules/{id}/trigger` | Manually trigger a run (202 if accepted) |

### Scheduler notes
- `spring.batch.job.enabled=false` — jobs never auto-run on startup.
- `spring.batch.jdbc.initialize-schema=always` — Spring Batch creates its own metadata tables in H2.
- `scheduling.enabled=true` — set to `false` to disable the cron poller without stopping the service.
- `lastRunTimes` is an in-memory map; on restart, schedules that fired during downtime will not be replayed.
- H2 database: `jdbc:h2:mem:batchdb`.

---

## Cross-cutting concerns

### Error responses (all services)
```json
{ "status": 400, "error": "Bad Request", "message": "...", "details": ["field: message"] }
```
`GlobalExceptionHandler` in `core` handles `BadRequestException` (400), `ResourceNotFoundException` (404), bean validation errors (400), and generic exceptions (500).

### Databases (local dev)
| Service | JDBC URL |
|---|---|
| template-service | `jdbc:h2:mem:templatedb` |
| ingestion-service | `jdbc:h2:mem:ingestiondb` |
| pipeline-service | `jdbc:h2:mem:pipelinedb` |
| batch-service | `jdbc:h2:mem:batchdb` |

All use `ddl-auto=update` and log SQL. PostgreSQL driver is on the classpath in all services — switch `spring.datasource.*` for production.

### Language
All validation error messages are in Portuguese.

### ObjectMapper
`spring-boot-starter-webmvc` in Spring Boot 4 does not auto-configure `ObjectMapper`. Each service that needs it declares an explicit `@Bean` in a `JacksonConfig` class with `JavaTimeModule` registered (so `LocalDate`/`LocalDateTime` serialise as ISO strings, not arrays).
