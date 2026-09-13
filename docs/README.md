# StaffAlias documentation

This directory is the source of truth for StaffAlias architecture, development practices, data modelling, deployment configuration, and engineering decisions.

## Architecture

- [Architecture overview](architecture/overview.md)
- [Data model](architecture/data-model.md)
- [Staff lifecycle domain](architecture/staff-lifecycle-domain.md)
- [Multi-tenancy](architecture/multi-tenancy.md)
- [Configuration profiles](architecture/configuration-profiles.md)

## Development

- [Getting started](development/getting-started.md)
- [Database, Flyway, Docker Compose, and Testcontainers](development/database.md)
- [Testing strategy](development/testing.md)

## Architecture decisions

- [ADR index](adr/README.md)
- [ADR 0001 — PostgreSQL with relational tables and JSONB](adr/0001-postgresql-and-jsonb.md)

## Documentation rules

Documentation is part of the product. Architectural changes, new cross-cutting conventions, and changes to developer workflow should update these documents in the same pull request where practical.

Use ADRs for decisions that affect multiple modules or establish durable technical constraints. Use architecture documents for the current intended design and development documents for executable setup/testing guidance.
