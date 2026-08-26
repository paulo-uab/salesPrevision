# core

Shared library consumed by all six Java services (`gateway-service`,
`user-service`, `template-service`, `ingestion-service`, `pipeline-service`,
`batch-service`). **Not a runnable service** — the `spring-boot-maven-plugin`
repackage step is explicitly skipped in `core/pom.xml` (`<skip>true</skip>`)
because turning it into an executable fat jar would nest its classes under
`BOOT-INF/classes/`, breaking every other module's dependency resolution. The
`CoreApplication` class in this module is a Spring Initializr stub only and is
never deployed.

## What lives here

| Package | Contents |
|---|---|
| `enums` | Shared enums used across services: `FileType`, `IngestionStatus`, `ValidationStatus`, `FieldDataType`, `TemplateRuleType`, `TransformationType`, `FilterOperator`, `LogicalOperator`, `BatchExecutionStatus`, `ServiceRole`, `ForecastFieldRole`. |
| `exception` | `BadRequestException` (400), `ResourceNotFoundException` (404), `ServiceUnavailableException` (503, thrown by client circuit-breaker fallbacks), and `GlobalExceptionHandler`/`GlobalExceptionHandlerAutoConfiguration`, which every service picks up automatically to render the standard `{status, error, message, details}` error body. |
| `correlation` | `CorrelationIdFilter` (stamps/propagates an `X-Correlation-Id` on every inbound request, put into the log MDC) and `CorrelationIdRestClientInterceptor` (forwards it on outbound `RestClient` calls), wired in automatically by `CorrelationIdAutoConfiguration`. |
| `serviceauth` | `ServiceTokenProvider` — logs into `/auth/login` with a machine account and caches the JWT until shortly before expiry — plus `ServiceAuthRestClientInterceptor` and `ServiceAuthAutoConfiguration`. Used for outbound calls authenticated as a dedicated internal service account (no end-user context available), e.g. cron-triggered calls in `batch-service`. |
| `tokenrelay` | `TokenRelayRestClientInterceptor`/`TokenRelayAutoConfiguration` — forwards the *inbound* request's `Authorization` header onto outbound `RestClient` calls, so the downstream service sees the original caller's identity (and `companyId`) instead of a generic machine identity. Used by `ingestion-service` and `pipeline-service` for their calls to `template-service`. |

### `serviceauth` vs. `tokenrelay` — which one applies where

Both auto-configurations attach a bearer token to outbound `RestClient` calls,
but they answer different questions and are opt-in via different properties:

- `tokenrelay` (`services.auth.relay=true`) is for calls made **on behalf of
  the current inbound request** — it only works inside a real HTTP request
  being handled by a controller; there's no token to relay from scheduled or
  background work.
- `serviceauth` (`services.gateway.url=...`) is for calls with **no inbound
  request to relay from** — background/scheduled work authenticates as a
  fixed machine account instead.

Neither should be wired into a service whose `RestClient` also calls
arbitrary, user-configured URLs — that would leak an internal token to a
third party. `batch-service` calls a user-configured prediction API URL, so
it deliberately does **not** use `core`'s `ServiceAuthAutoConfiguration`;
instead it defines its own `InternalServiceAuthConfig` and applies the
interceptor by hand only to the specific `RestClient` beans that call
`pipeline-service`/`ingestion-service` directly (see
`batch-service/src/main/java/com/uab/salesprevision/batch/config/InternalServiceAuthConfig.java`).

## Note on inter-service DTOs

Earlier documentation described a `dto/ingestion`, `dto/template` and
`dto/pipeline` package living in `core`. That is no longer the case: each
consuming service now keeps its own client-local DTOs under
`<service>/src/main/java/.../client/dto/` (e.g. `TemplateClientDto` in both
`ingestion-service` and `pipeline-service`, `PipelineClientDto`/
`IngestionJobDto` in `batch-service`). `core` itself no longer has a `dto`
package.

## Building

This module has no `main` to run. It's built as a dependency of the other
modules:

```bash
./mvnw -f core/pom.xml clean install -DskipTests
```

Every service's own `Dockerfile` performs exactly this step before building
the service itself (see any service's `Dockerfile` for the two-stage build).
