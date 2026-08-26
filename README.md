# SalesPrevision

A multi-tenant microservices platform that ingests tabular sales data, lets you
define reusable transformation pipelines over it, and runs scheduled batch
jobs that forecast future sales with a dedicated Python prediction service.

Built as a Spring Boot 4 / Java 17 microservice suite (5 services + a shared
library) plus a FastAPI (Python 3.12) forecasting service, fronted by a single
API gateway and secured end-to-end with JWT.

## Architecture

```
                                   ┌─────────────┐
                                   │   Client    │
                                   └──────┬──────┘
                                          │ HTTPS
                                          ▼
                              ┌───────────────────────┐
                              │   gateway-service      │  :8080
                              │  Spring Cloud Gateway  │  routes + validates JWTs
                              │  + Resilience4j CBs    │  (13 routes to 6 services)
                              └───────────┬───────────┘
                                          │
              ┌─────────────┬─────────────┼─────────────┬──────────────┐
              ▼             ▼             ▼             ▼              ▼
      ┌───────────────┐┌──────────┐┌──────────────┐┌───────────┐┌─────────────┐
      │ user-service  ││ template ││  ingestion   ││ pipeline  ││ prediction  │
      │    :8086      ││ -service ││  -service    ││ -service  ││ -service    │
      │               ││  :8081   ││   :8082      ││  :8083    ││   :8085     │
      │ Companies,    ││          ││              ││           ││ (Python /   │
      │ users, roles. ││ Defines  ││ Upload +     ││ Field     ││  FastAPI)   │
      │ Issues JWTs   ││ file     ││ validate     ││ selection,││ Forecasting │
      │ (/auth/login).││ shapes.  ││ files async  ││ filters,  ││ models:     │
      │ Publishes     ││          ││ via Kafka.   ││ EXOG/     ││ baseline,   │
      │ JWKS.         ││          ││              ││ TARGET/   ││ statistical,│
      │               ││          ││              ││ DATE/     ││ ML.         │
      │               ││          ││              ││ GROUP     ││             │
      │               ││          ││              ││ roles.    ││             │
      └───────────────┘└──────────┘└──────┬───────┘└─────┬─────┘└──────▲──────┘
                                          │  Kafka        │             │
                                          ▼               │             │
                                   ┌─────────────┐         │             │
                                   │    kafka     │        │             │
                                   │ (KRaft mode) │        │             │
                                   └─────────────┘         │             │
                                                            │             │
                                          ┌─────────────────┘             │
                                          ▼                                │
                                  ┌───────────────┐                        │
                                  │ batch-service  │  reads ingested data,  │
                                  │    :8084       │  applies a pipeline,   │
                                  │ Spring Batch + │  POSTs the result ─────┘
                                  │ cron scheduler │  (to prediction-service
                                  └───────────────┘   or any external URL)
```

`core` is a shared library (not a runnable service) consumed by all six Java
modules: DTOs, enums, exceptions, i18n plumbing, and the correlation-id /
service-auth / token-relay infrastructure used for inter-service calls.

## Modules

| Module | Port | Purpose |
|---|---|---|
| [`core`](core/README.md) | — | Shared library: DTOs, enums, exceptions, correlation IDs, service-auth & token-relay infrastructure. Not a runnable service. |
| [`gateway-service`](gateway-service/README.md) | 8080 | Entry point. Validates JWTs against user-service's JWKS and routes to every other service behind a Resilience4j circuit breaker. |
| [`user-service`](user-service/README.md) | 8086 | Companies, users, per-service roles. Issues JWTs (RS256) and publishes the JWKS every other service verifies against. |
| [`template-service`](template-service/README.md) | 8081 | Defines the expected shape (columns, types, validation rules) of files that can be ingested. |
| [`ingestion-service`](ingestion-service/README.md) | 8082 | Accepts file uploads, validates them against a template, and stores the resulting records — processed asynchronously via Kafka. |
| [`pipeline-service`](pipeline-service/README.md) | 8083 | Defines forecast pipelines: which fields to extract/transform/filter, and which field plays which role (date, target, group, exogenous) in the forecast. |
| [`batch-service`](batch-service/README.md) | 8084 | Runs scheduled Spring Batch jobs that read ingested data, apply a pipeline, and send the result to a prediction API. |
| [`prediction-service`](prediction-service/README.md) | 8085 | Python/FastAPI service that runs baseline, statistical and ML forecasting models over the data it's sent. |

## Prerequisites

- Java 17
- Maven (or just use the bundled `./mvnw` — no local Maven install required)
- Docker Desktop, if you want to run the whole stack with one command
- Python 3.12, only if you want to run `prediction-service` outside Docker

## Running it

### Option 1 — locally, one service at a time

Each Java service defaults to the `dev` Spring profile (embedded H2, console
at `/h2-console`) whenever `SPRING_PROFILES_ACTIVE` isn't set.

```bash
# from the repo root, build everything once
./mvnw clean package

# then, in separate terminals, from each service's own directory:
cd user-service     && ../mvnw spring-boot:run   # start this one first
cd gateway-service   && ../mvnw spring-boot:run
cd template-service  && ../mvnw spring-boot:run
cd ingestion-service && ../mvnw spring-boot:run
cd pipeline-service  && ../mvnw spring-boot:run
cd batch-service     && ../mvnw spring-boot:run

# and, for the Python service (see prediction-service/README.md for details):
cd prediction-service && uvicorn app.main:app --port 8085
```

**Start `user-service` first.** Every other service validates inbound JWTs
against the RSA public key `user-service` publishes at
`/.well-known/jwks.json` — if it isn't up yet, the first request to any other
service will fail to resolve a `JwtDecoder`. `ingestion-service` also needs a
running Kafka broker to process uploaded files (see `docker-compose.yml` for
a standalone Kafka container, or point `spring.kafka.bootstrap-servers` at
one of your own).

### Option 2 — everything via Docker Compose

```bash
docker compose up -d --build
```

This builds and starts Kafka plus all six Java services and `prediction-service`
in one shot, each activated with the `docker` Spring profile (in addition to
`dev`) — `docker` only overrides the inter-service URLs and Kafka bootstrap
servers to point at Compose service names instead of `localhost`, so H2 stays
the datastore either way. `docker-compose.yml` also encodes the startup order
described above via `depends_on`.

## Further reading

- [`CLAUDE.md`](CLAUDE.md) — the most detailed reference for each service's data model, endpoints and behavior.
- [`docs/technical-documentation.md`](docs/technical-documentation.md) — technical documentation (Portuguese).
- [`docs/pipeline-forecast-configuration.md`](docs/pipeline-forecast-configuration.md) — field-role (DATE/TARGET/GROUP/EXOG) and model configuration guide for `pipeline-service` and `prediction-service`.
