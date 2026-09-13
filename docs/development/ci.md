# Continuous integration

StaffAlias CI is defined in `.github/workflows/ci.yml` and runs for pull requests targeting `main` and pushes to `main`.

## Backend job

The backend job uses Java 21 and Maven. It runs:

```bash
cd backend
mvn --batch-mode --no-transfer-progress verify
```

The job executes unit tests and PostgreSQL Testcontainers integration tests. Testcontainers starts PostgreSQL on the GitHub-hosted runner, so the suite validates the same database engine used by the application.

`PostgresIntegrationTest` verifies that a clean PostgreSQL database can run all Flyway migrations and that the expected core tables are created.

## Frontend job

The frontend job uses Node.js 22 and runs:

```bash
cd frontend
npm install --no-audit --no-fund
npm run lint
npm run typecheck
npm run test
npm run build
```

The frontend currently has no committed package lockfile, so CI intentionally uses `npm install` instead of `npm ci`. The npm download cache is keyed from `frontend/package.json`.

CI supplies `VITE_API_URL=http://localhost:8080` as a non-secret build/test value. The quality checks do not require a live backend.

## Caching

- Maven dependency caching is handled by `actions/setup-java`.
- npm's download cache is stored through `actions/cache`.
- generated application build artifacts are not cached, so every workflow performs fresh compilation/build steps.

## Secrets

The workflow does not require application secrets, database passwords, or developer-local environment files. Testcontainers provisions its own ephemeral PostgreSQL instance.

## Completion rule

A pull request that changes backend or frontend behavior is not complete until the relevant CI jobs are green. Do not bypass a failing check; reproduce the command locally and fix its root cause.
