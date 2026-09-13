# Configuration profiles

StaffAlias separates common configuration from environment-specific configuration.

## Backend

Spring Boot common settings live in `backend/src/main/resources/application.yml`.

Environment-specific settings live in:

- `application-dev.yml` for local development;
- `application-prod.yml` for deployed production use.

The `dev` profile may use safe local defaults that match the Docker Compose PostgreSQL service. The `prod` profile must obtain database connection details and secrets from environment variables or the deployment platform's secret store.

Production credentials must never be committed.

Hibernate is configured to validate the database schema. Flyway is responsible for schema changes.

## Frontend

The frontend is built with Vite. Environment-specific values are supplied through Vite environment variables, primarily `VITE_API_URL`.

For local development, copy `frontend/.env.example` to `frontend/.env.local` and set the API URL to the locally running backend.

Production builds must receive `VITE_API_URL` from deployment configuration. Application source must not contain development or production endpoint fallbacks.

## Principles

- common behavior belongs in version-controlled application configuration;
- environment-specific endpoints and credentials are externalized;
- secrets are never stored in repository files;
- `dev` should optimize local usability without changing domain behavior;
- `prod` should fail clearly when required environment configuration is missing rather than silently using development values.
