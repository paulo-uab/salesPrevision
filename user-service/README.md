# user-service

Multi-tenant identity and authorization service. Owns companies, users and
their per-service roles; issues the RS256 JWTs that authenticate every
request in the system, and publishes the public key other services verify
those tokens against.

**Port:** 8086

## Entities

| Entity | Notes |
|---|---|
| `Company` | `id`, unique `name`. A user always belongs to exactly one company; a company must exist before a user can be created in it. |
| `User` | `id`, unique `username`, `passwordHash` (BCrypt), `company` (eager `ManyToOne`), `roles` (`Set<ServiceRole>`, eager element collection), `active`. |

`ServiceRole` (`core.enums.ServiceRole`) is the full set of per-service
authorities a user can hold: `TEMPLATE_READ/EDIT/EXECUTE`,
`INGESTION_READ/EDIT/EXECUTE`, `PIPELINE_READ/EDIT/EXECUTE`,
`BATCH_READ/EDIT/EXECUTE`, `USER_READ/EDIT`. There is no separate admin
role — an "admin" is simply a user holding every value.

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/login` | public | The only public endpoint besides JWKS, health and docs. Returns 401 on a wrong username/password or inactive account; otherwise a JWT (RS256, subject = username, `roles` claim, `companyId` claim) and its expiry. |
| GET | `/.well-known/jwks.json` | public | Standard JWKS document (RFC 7517). Every resource server in the system (all other Java services, and `prediction-service` via `PyJWKClient`) polls this to validate tokens by `kid`. |
| POST | `/api/companies` | `USER_EDIT` | Create a company. |
| GET | `/api/companies` | `USER_READ` | List all companies. |
| GET | `/api/companies/{id}` | `USER_READ` | Get a company by ID. |
| POST | `/api/users` | `USER_EDIT` | Create a user. The referenced `companyId` must already exist; password is hashed before storage. |
| GET | `/api/users` | `USER_READ` | List all users. |
| GET | `/api/users/{id}` | `USER_READ` | Get a user by ID. |
| PUT | `/api/users/{id}` | `USER_EDIT` | Update a user's company and roles (username/password unchanged). |
| PATCH | `/api/users/{id}/active` | `USER_EDIT` | Activate/deactivate a user. Inactive users can't log in; historical data is unaffected. |

## Role gating

Enforced in `SecurityConfig` by HTTP method: `GET /api/users/**` and
`GET /api/companies/**` require `USER_READ`; every mutating verb
(`POST`/`PUT`/`PATCH`) on either resource requires `USER_EDIT`. `/auth/login`
and `/.well-known/jwks.json` are the only public paths besides
`/actuator/**`, Swagger and the H2 console.

## Signing

This is the only service in the codebase that holds RSA key material — every
other service (including `gateway-service`) only *validates* tokens against
the public key this service exposes. The dev key pair lives in
`application-dev.properties`/`application.properties`
(`app.jwt.private-key`/`app.jwt.public-key`, base64-encoded DER) — replace it
for anything beyond local development.

## Data seeding

On startup, `DataSeeder` seeds a `Default Company` and three accounts if they
don't already exist: `admin`/`admin123` (every `ServiceRole`),
`user`/`user123` (read-only across all four domain services), and
`internal-service`/`internal-service123` (`INGESTION_READ`, `PIPELINE_READ`
— the machine account `batch-service` authenticates as for its direct,
cron-triggered calls to `ingestion-service` and `pipeline-service`).

## Calls to other services

None — `user-service` is a leaf service. It issues and validates its own
tokens against itself.

## Running standalone

```bash
cd user-service && ../mvnw spring-boot:run
```

Defaults to the `dev` profile (H2, `jdbc:h2:mem:userdb`, console at
`/h2-console`). No `docker` profile properties file exists for this service —
unlike the others, it has no inter-service URLs to repoint for Compose.
