# Architecture overview

StaffAlias is a multi-tenant staff lifecycle application implemented as a monorepo. The initial architecture is a modular monolith: a Spring Boot backend, a React frontend, PostgreSQL, and documentation maintained in the same repository.

## Top-level structure

- `backend/` — Spring Boot application, domain modules, Flyway migrations, and backend tests.
- `frontend/` — React 19 + TypeScript + Vite + Refine Core + Ant Design application.
- `docs/` — architecture, development guidance, and architecture decision records.
- `docker-compose.yml` — local PostgreSQL infrastructure.
- `Makefile` — root-level development shortcuts.

## Backend architecture

The backend is organized by business/domain module instead of technical layer. Current and planned modules include tenant, people, employment, organisation, lifecycle, configuration, and audit.

The first domain model is:

```text
Tenant
  └── Person
        └── Employment
              └── EmploymentIdentifier (EMPLOYEE_ID)
```

Technical UUIDs are used for primary and foreign keys. Business identifiers such as `employee_id` are values in the domain and are never used as database relationships.

## Frontend architecture

The frontend uses React 19, TypeScript, Vite, Refine Core v5, Ant Design v6, and React Router. Refine is used headlessly for resource/data-provider concerns while Ant Design is used directly for the UI layer.

Frontend API endpoints are supplied through environment variables. Authentication and tenant state are represented by abstractions so the application is not coupled to a particular identity provider.

## Persistence

PostgreSQL is the database for development, tests, and production-oriented deployments. Flyway owns schema evolution. Hibernate/JPA validates the schema instead of mutating it automatically.

The initial schema is relational. Configurable or jurisdiction-specific attributes are expected to use PostgreSQL JSONB where appropriate, guided by metadata rather than turning the core model into an EAV schema.

## Multi-tenancy

The initial model uses one PostgreSQL database and one shared schema. Tenant-owned rows carry `tenant_id`; application services resolve the active tenant through `TenantContext` and repository/service methods scope access by tenant. Database keys and uniqueness constraints include tenant ownership where required.

See [multi-tenancy.md](multi-tenancy.md) for the detailed rules.

## Evolution principles

StaffAlias should remain a modular monolith until scaling or organizational boundaries justify decomposition. New tenant-owned aggregates must explicitly define tenant ownership, effective dating should be used where historical truth matters, and architectural decisions that affect multiple modules should be recorded as ADRs.
