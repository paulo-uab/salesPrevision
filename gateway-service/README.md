# gateway-service

Single entry point for the whole system. Pure validate-and-route: built on
Spring Cloud Gateway (WebFlux), it verifies every inbound JWT against
`user-service`'s published JWKS and proxies to the six other services, each
route wrapped with a Resilience4j circuit breaker. It holds no key material
and issues no tokens of its own — that changed from an earlier design where
this service used to sign tokens itself.

**Port:** 8080

## Routes

13 routes are configured in `application.properties`
(`spring.cloud.gateway.server.webflux.routes[0..12]`):

| # | Route id | Predicate | Target | Notes |
|---|---|---|---|---|
| 0–3 | `*-service-docs` | `Path=/v3/api-docs/{service}` | template/ingestion/pipeline/batch-service | Rewrites to the downstream's `/v3/api-docs`, aggregated into one Swagger UI. |
| 4 | `template-service` | `/v1/api/templates/**` | template-service:8081 | Circuit breaker `template-service`. |
| 5 | `ingestion-service` | `/v1/api/ingestion/**` | ingestion-service:8082 | Circuit breaker `ingestion-service`. |
| 6 | `pipeline-service` | `/v1/api/pipelines/**` | pipeline-service:8083 | Circuit breaker `pipeline-service`. |
| 7 | `batch-service` | `/api/batch/**` | batch-service:8084 | Circuit breaker `batch-service`. |
| 8 | `prediction-forecast` | `/api/forecast/**` | prediction-service:8085 | Circuit breaker `prediction-service`. |
| 9 | `prediction-config` | `/api/config/**` | prediction-service:8085 | Circuit breaker `prediction-service`. |
| 10 | `prediction-models` | `/api/models/**` | prediction-service:8085 | Circuit breaker `prediction-service`. |
| 11 | `user-service-auth` | `POST /auth/login` | user-service:8086 | Circuit breaker `user-service`. Public — no JWT required. |
| 12 | `user-service-management` | `/api/users/**`, `/api/companies/**` | user-service:8086 | Circuit breaker `user-service`. |

Every open circuit breaker forwards to `GET/POST /fallback/service-unavailable`
(`FallbackController`), which returns a 503 with the underlying exception's
message.

## Security

`SecurityConfig` (WebFlux) permits `POST /auth/login`, `/.well-known/jwks.json`,
`/actuator/**`, `/fallback/**` and Swagger/OpenAPI paths; everything else
requires a valid JWT, verified via
`spring.security.oauth2.resourceserver.jwt.jwk-set-uri` pointed at
`user-service`'s JWKS endpoint. No route-specific `ServiceRole` gating happens
here — each downstream service enforces its own roles once the request is
proxied through.

## Resilience4j

Default circuit-breaker config: sliding window of 10 calls, minimum 5 calls
before evaluating, 50% failure-rate threshold, 80% slow-call-rate threshold
(3s slow-call duration), 15s wait in open state, 3 permitted calls in
half-open state, automatic half-open transition. Instances are declared for
`template-service`, `ingestion-service`, `pipeline-service`, `batch-service`,
`prediction-service` and `user-service`.

Rate limiting (`RequestRateLimiter`, Redis-backed) was removed for local dev
to avoid requiring a running Redis instance — see the comment above the
`default-filters` property in `application.properties` for how to reintroduce
it.

## Calls to other services

Routes to all six other services (`user-service`, `template-service`,
`ingestion-service`, `pipeline-service`, `batch-service`,
`prediction-service`) — it never calls anything itself outside of proxying.

## Running standalone

```bash
cd gateway-service && ../mvnw spring-boot:run
```

No Spring profile split (`dev`/`docker`) beyond the routes' target hostnames:
`application-docker.properties` overrides only the 13 routes' `uri` (and the
JWKS `jwk-set-uri`) to point at Compose service names instead of `localhost`.
This service has no datasource of its own.
