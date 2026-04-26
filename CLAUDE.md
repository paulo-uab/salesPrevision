# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Build
./mvnw clean package

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ClassName

# Clean build artifacts
./mvnw clean
```

The H2 console is available at `/h2-console` when running locally. File upload limit is 20MB.

## Architecture

Spring Boot 4.0 / Java 17 / Maven application for ingesting and validating tabular data files (CSV, JSON, XLSX, TXT) against configurable templates.

### Request flow

```
HTTP Request
  → Controller (/api/templates, /api/ingestion/jobs)
  → Service (business logic + transaction boundary)
  → IngestionProcessorFactory → IngestionProcessor (strategy per FileType)
  → AbstractIngestionProcessor (validation + type conversion)
  → Repository (JPA → H2 dev / PostgreSQL prod)
```

### Data model

```
IngestionTemplate
  ├── TemplateField (1-to-many)
  │   └── TemplateValidationRule (1-to-many)  ← NOT_NULL, REGEX, MIN, MAX, ENUM, DATE_FORMAT
  └── IngestionJob (1-to-many)
        ├── IngestedRecord  ← one row per source line, status VALID/INVALID
        └── IngestionError  ← one row per failed rule, with field/line/raw value
```

### Key packages

| Package | Responsibility |
|---|---|
| `controller/` | REST endpoints, request/response mapping |
| `service/` | Orchestration, file storage (UUID prefix + SHA-256 checksum), job lifecycle |
| `service/ingestion/` | `IngestionProcessorFactory` selects processor; `AbstractIngestionProcessor` handles field extraction, default values, type conversion, rule validation |
| `entity/` | JPA entities with audit fields (`createdAt`, `createdBy`, `updatedAt`) |
| `repository/` | Spring Data JPA repositories |
| `dto/` | Request/response DTOs |
| `exception/` | `BadRequestException` (400), `ResourceNotFoundException` (404); `GlobalExceptionHandler` returns `{status, error, message, details}` |

### Adding a new file format

1. Implement `IngestionProcessor` (or extend `AbstractIngestionProcessor`).
2. Register it in `IngestionProcessorFactory` mapped to the new `FileType` enum value.

### Database

- **Development**: H2 in-memory (`jdbc:h2:mem:forecastdb`), schema auto-updated, SQL logged.
- **Production**: PostgreSQL driver is on the classpath; switch `spring.datasource.*` properties.

### Security

OAuth2 Client is configured but not fully wired for local dev — requests are unauthenticated by default against the in-memory database. Redis/JDBC session management is available as a dependency.

### Language note

Validation error messages are in Portuguese throughout the codebase.
