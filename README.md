# StaffAlias

StaffAlias is a multi-tenant staff lifecycle application for tracking people from hire through employment changes to leaving and rehire.

## Repository layout

```text
StaffAlias/
├── backend/          # Spring Boot backend (implemented in issue #2)
├── frontend/         # React/TypeScript frontend (implemented in issue #5)
├── docs/             # Architecture and developer documentation
├── docker-compose.yml
├── .env.example
├── .gitignore
└── Makefile
```

The project is intentionally a monorepo so application code, documentation, local infrastructure, and shared development conventions evolve together.

## Architecture principles

- Multi-tenancy is a first-class requirement. Tenant-owned data must always be designed with tenant isolation in mind.
- PostgreSQL is the target database for development, tests, and production-oriented deployments.
- Backend and frontend configuration must support separate `dev` and `prod` environments without duplicating application code.
- Secrets must be supplied through environment variables or deployment secret stores and must never be committed.
- Technical identifiers are used for database relationships; business identifiers such as `employee_id` are not foreign keys.
- Documentation under [`docs/`](docs/) is part of the product and should be updated with architectural changes.

## Getting started

The repository is currently being bootstrapped. Framework-specific setup is tracked separately:

- Backend and profiles: issue #2
- PostgreSQL, Flyway, Docker Compose, and Testcontainers: issue #3
- Multi-tenancy foundation: issue #4
- Frontend and environment configuration: issue #5
- Initial staff lifecycle domain model: issue #6
- Documentation expansion: issue #7
- CI: issue #8

Copy `.env.example` to `.env` for local development once the relevant services are implemented. `.env` is ignored by Git.

Run `make help` to see the root-level developer commands currently available.

## Naming conventions

- Java packages: lowercase, rooted under a StaffAlias application package and organised by domain/module.
- TypeScript files/components: follow the conventions established by the frontend bootstrap issue.
- Environment variables: uppercase `SNAKE_CASE`, prefixed with `STAFFALIAS_` for application-level settings where practical.
- Database objects: lowercase `snake_case`.
- Git feature branches: descriptive names tied to issues, for example `issue-1-bootstrap-monorepo`.

## Configuration

Common configuration belongs in application code/configuration shared by all environments. Environment-specific values belong in the `dev` or `prod` configuration profile and should be externalised through environment variables where appropriate.

Production secrets, passwords, tokens, and credentials must never be committed to this repository.
