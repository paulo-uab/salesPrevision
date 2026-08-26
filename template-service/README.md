# template-service

Defines *ingestion templates*: the expected shape (columns, types, validation
rules) of files that `ingestion-service` will later validate uploads against.
A leaf service — it never calls anything else.

**Port:** 8081

## Data model

```
IngestionTemplate  (company_id + name unique per company)
  └── TemplateField (1-to-many, cascade all, orphan removal)
        └── TemplateValidationRule (1-to-many, cascade all, orphan removal)
```

`IngestionTemplate` fields: `id`, `companyId`, `name`, `description`,
`fileType` (`FileType`: CSV/JSON/XLSX/TXT/UNKNOWN), `delimiter`, `hasHeader`,
`sheetName`, `rootPath`, `version`, `active`, `createdAt`/`updatedAt`
(`@CreatedDate`/`@LastModifiedDate`).

`TemplateField` and `TemplateValidationRule` are as documented in
`CLAUDE.md` — canonical field name, source column/key, `FieldDataType`,
required/positional fallback, and per-field `TemplateRuleType` rules
(NOT_NULL, REGEX, MIN, MAX, ENUM, DATE_FORMAT).

## Endpoints

| Method | Path | Role required | Description |
|---|---|---|---|
| POST | `v1/api/templates` | `TEMPLATE_EDIT` | Create a template. |
| PUT | `v1/api/templates/{id}` | `TEMPLATE_EDIT` | Replace a template's definition (name, file settings, entire field list); bumps `version`. |
| PATCH | `v1/api/templates/{id}/active` | `TEMPLATE_EDIT` | Activate/deactivate a template. Inactive templates can't be used for new ingestion jobs; existing jobs/records are unaffected. |
| GET | `v1/api/templates` | `TEMPLATE_READ` | Paginated list, scoped to the caller's company. Optional `active` filter. |
| GET | `v1/api/templates/{id}` | `TEMPLATE_READ` | Get a template by ID. |

`PUT` and `PATCH .../active` are not mentioned in `CLAUDE.md`'s original
endpoint table — they exist in the current controller.

## Role gating & multi-tenancy

`SecurityConfig` requires `TEMPLATE_READ` for every `GET`, and
`TEMPLATE_EDIT` for `POST`/`PUT`/`PATCH`, based on the JWT's `roles` claim.
Company-level data isolation is a separate check, layered on top in
`IngestionTemplateService`: every write stamps the caller's `companyId`
(from the JWT claim) onto the entity, and `name` uniqueness plus every read
are scoped to that same `companyId` — one company can never see or collide
with another's templates.

## Calls to other services

None — this is a leaf service, called by `ingestion-service` and
`pipeline-service` but never a caller itself.

## Running standalone

```bash
cd template-service && ../mvnw spring-boot:run
```

Defaults to the `dev` profile (H2, `jdbc:h2:mem:templatedb`, console at
`/h2-console`). The `docker` profile only repoints the JWKS
`jwk-set-uri` at `user-service`'s Compose hostname.
