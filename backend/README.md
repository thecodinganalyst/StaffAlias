# Backend

Spring Boot backend for StaffAlias.

## Technology

- Java 21 LTS
- Spring Boot 3.5.16
- Spring Web
- Bean Validation
- Spring Data JPA
- PostgreSQL driver
- Flyway
- Spring Boot Actuator
- Maven

## Profiles

The backend uses explicit Spring profiles:

- `dev` - local development. Defaults to PostgreSQL at `jdbc:postgresql://localhost:5432/staffalias` with local-only `staffalias` credentials. Issue #3 provides the Docker Compose database.
- `prod` - production/deployed runtime. `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are mandatory environment variables and have no committed secret defaults.

Common configuration lives in `application.yml`. Profile-specific settings live in `application-dev.yml` and `application-prod.yml`.

## Run

From `backend/`:

```bash
mvn clean test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The dev application requires a reachable PostgreSQL database. The reproducible local PostgreSQL environment is tracked in issue #3.

For production-style startup:

```bash
DB_URL=jdbc:postgresql://host:5432/staffalias \
DB_USERNAME=staffalias \
DB_PASSWORD=replace-me \
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Health

Spring Boot Actuator exposes:

```text
GET /actuator/health
```

Only `health` and `info` are exposed over HTTP initially.

## Domain modules

Code is organised by business domain rather than generic technical layers. Initial module boundaries are:

- `tenant`
- `people`
- `employment`
- `organisation`
- `lifecycle`
- `configuration`
- `audit`

Each module can contain its own API, application/service, domain, and persistence concerns as it grows. Tenant-awareness is a cross-cutting invariant and will be implemented in issue #4.
