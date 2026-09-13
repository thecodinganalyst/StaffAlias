# StaffAlias

StaffAlias is a multi-tenant staff lifecycle application for tracking people from hire through employment changes to leaving and rehire.

## Repository layout

```text
StaffAlias/
├── backend/          # Spring Boot backend
├── frontend/         # React/TypeScript frontend
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
- Backend and frontend configuration support separate `dev` and `prod` environments without duplicating application code.
- Secrets must be supplied through environment variables or deployment secret stores and must never be committed.
- Technical identifiers are used for database relationships; business identifiers such as `employee_id` are not foreign keys.
- Documentation under [`docs/`](docs/) is part of the product and should be updated with architectural changes.

## Documentation

Start with the [documentation index](docs/README.md).

Key references:

- [Architecture overview](docs/architecture/overview.md)
- [Data model](docs/architecture/data-model.md)
- [Multi-tenancy](docs/architecture/multi-tenancy.md)
- [Configuration profiles](docs/architecture/configuration-profiles.md)
- [Getting started](docs/development/getting-started.md)
- [Database guide](docs/development/database.md)
- [Testing strategy](docs/development/testing.md)
- [Architecture Decision Records](docs/adr/README.md)

## Getting started

Follow [docs/development/getting-started.md](docs/development/getting-started.md) for prerequisites and local startup.

Run `make help` to see root-level developer commands.

## Naming conventions

- Java packages: lowercase, rooted under a StaffAlias application package and organised by domain/module.
- TypeScript files/components: follow the conventions established by the frontend.
- Environment variables: uppercase `SNAKE_CASE`, prefixed with `STAFFALIAS_` for application-level settings where practical.
- Database objects: lowercase `snake_case`.
- Git feature branches: descriptive names tied to issues, for example `issue-7-documentation`.

## Configuration

Common configuration belongs in application code/configuration shared by all environments. Environment-specific values belong in the `dev` or `prod` configuration profile and should be externalised through environment variables where appropriate.

Production secrets, passwords, tokens, and credentials must never be committed to this repository.
