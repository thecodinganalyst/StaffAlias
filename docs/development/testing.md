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

Tests should create their own isolated database containers and must not depend on the developer's local Compose database already running.

## Frontend tests

The frontend uses Vitest and React Testing Library.

Run the complete frontend quality suite with:

```bash
cd frontend
npm run lint
npm run typecheck
npm run test
npm run build
```

Tests should focus on observable behavior, routing, error/fallback behavior, and integration boundaries rather than implementation details.

## Local manual checks

When changing an end-to-end flow, also verify the application locally with PostgreSQL, backend, and frontend running together. Check browser console/network errors and responsive layout where UI behavior is affected.

## CI

Issue #8 introduces GitHub Actions so backend tests, PostgreSQL integration tests, frontend lint/typecheck/tests, and the production frontend build run automatically for pull requests and pushes to the main development branch.

A PR should not be treated as fully verified merely because code review looks correct; required automated commands should execute successfully in CI or an equivalent development environment.
