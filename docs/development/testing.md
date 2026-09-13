# Testing strategy

StaffAlias uses production-like technology in automated tests wherever practical.

## Backend tests

Backend tests use JUnit through Spring Boot's test support.

PostgreSQL-specific integration behavior is tested with Testcontainers rather than H2. This matters because the application relies on PostgreSQL features such as UUIDs, range/exclusion constraints, Flyway migrations, and eventually JSONB.

Run:

```bash
cd backend
mvn test
```

Docker must be available for Testcontainers-based tests.

Integration tests should verify both functional behavior and architecture invariants, especially:

- Flyway migrations apply successfully to a clean PostgreSQL instance;
- JPA mappings validate against the migrated schema;
- tenant-owned records cannot be accessed from another tenant context;
- business identifiers remain tenant scoped;
- effective-dated rules such as employee-ID overlap prevention work in PostgreSQL.

`PostgresIntegrationTest` starts a clean PostgreSQL 17 Testcontainer, boots the application against it, verifies all current Flyway migrations succeed, and checks that the core lifecycle tables exist. This gives CI an explicit clean-database migration gate.

Tests create their own isolated database containers and must not depend on the developer's local Compose database already running.

## Frontend tests

The frontend uses Vitest and React Testing Library.

Run the complete frontend quality suite with:

```bash
cd frontend
npm install
npm run lint
npm run typecheck
npm run test
npm run build
```

`VITE_API_URL` must be configured for tests and builds. For local development, copy `frontend/.env.example` to `.env.local`. CI supplies a non-secret localhost placeholder because the frontend quality checks do not require a live backend.

Tests should focus on observable behavior, routing, error/fallback behavior, and integration boundaries rather than implementation details.

## Local manual checks

When changing an end-to-end flow, also verify the application locally with PostgreSQL, backend, and frontend running together. Check browser console/network errors and responsive layout where UI behavior is affected.

## Continuous integration

GitHub Actions runs `.github/workflows/ci.yml` for every pull request targeting `main` and every push to `main`.

The workflow has two independently reported jobs:

- **Backend / PostgreSQL integration** — sets up Java 21, restores the Maven dependency cache, and runs `mvn verify`. Testcontainers starts PostgreSQL inside the GitHub-hosted runner, so tenant isolation, lifecycle-domain tests, and clean Flyway migration checks run against the real database engine.
- **Frontend / quality and build** — sets up Node.js 22, restores the npm download cache, installs dependencies, then runs lint, type checking, Vitest, and the production build.

The workflow uses only repository source and public dependency registries. No developer-local credentials or application secrets are required.

A pull request should not be considered complete until both jobs are green. When a job fails, reproduce its command locally and fix the root cause rather than bypassing the check.

## Before opening or merging a PR

Run the affected checks locally where possible. For changes spanning backend and frontend, run both suites:

```bash
cd backend && mvn verify
cd ../frontend && npm install && npm run lint && npm run typecheck && npm run test && npm run build
```

CI is the final shared verification environment, especially for Testcontainers and clean Flyway migration execution.
