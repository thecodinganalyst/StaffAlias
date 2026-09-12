# Database development

StaffAlias uses PostgreSQL for local development, integration testing, and production-oriented configuration. Flyway owns schema changes; Hibernate validates the schema and does not mutate it automatically.

## Prerequisites

- Docker with Docker Compose
- Java 21
- Maven

## Start PostgreSQL

From the repository root:

```bash
make db-up
```

or:

```bash
docker compose up -d postgres
```

The default local connection is:

```text
jdbc:postgresql://localhost:5432/staffalias
username: staffalias
password: staffalias
```

These are development-only defaults. Override the Compose values with a local `.env` file based on `.env.example` when needed.

## Stop PostgreSQL

```bash
make db-down
```

This preserves the named PostgreSQL data volume.

## Reset PostgreSQL

```bash
make db-reset
```

This removes the local data volume and starts PostgreSQL again from an empty database. Flyway migrations will run when the backend next starts.

## Run the backend

Start PostgreSQL first, then:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile connects to local PostgreSQL by default. `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` can override the defaults.

## Flyway migrations

Migrations are stored in:

```text
backend/src/main/resources/db/migration/
```

Rules:

- never edit a migration that has already been merged/applied in shared environments;
- add a new versioned migration for every schema change;
- keep Hibernate `ddl-auto=validate` so schema drift fails visibly;
- domain tables should be introduced by the feature that owns them.

`V1__baseline.sql` establishes the migration history without introducing business-domain tables.

## Tests

Run:

```bash
make backend-test
```

Integration tests use Testcontainers to start an isolated PostgreSQL container. They do not depend on the local Compose database and therefore require Docker but no pre-existing database.

`PostgresIntegrationTest` verifies that the application connects to PostgreSQL and that the Flyway baseline migration has completed successfully.

## Production

The `prod` profile does not contain database credentials. Supply:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

through the deployment environment or secret-management mechanism. Do not reuse local development credentials in production.
