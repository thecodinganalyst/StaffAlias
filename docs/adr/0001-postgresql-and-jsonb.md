# ADR 0001: PostgreSQL with relational tables and JSONB

- Status: Accepted
- Date: 2026-09-13

## Context

StaffAlias must model strongly relational HR concepts such as tenants, people, employments, assignments, lifecycle history, and technical relationships. At the same time, different companies and jurisdictions require configurable fields that cannot all be known at compile time.

A fully relational fixed schema would make tenant/jurisdiction-specific attributes expensive to evolve. A document database or pure EAV model would make referential integrity, effective dating, reporting, transactions, and typed querying harder for core HR data.

## Decision

Use PostgreSQL as the application database for development, tests, and production-oriented deployments.

Model stable/core concepts as normal relational tables with technical primary/foreign keys and explicit constraints. Use PostgreSQL JSONB selectively for configurable/dynamic attributes where the shape is metadata-driven and does not justify first-class relational columns.

Flyway owns schema evolution. Automated integration tests use PostgreSQL through Testcontainers instead of substituting H2.

## Consequences

Positive consequences:

- strong relational integrity for core HR concepts;
- transactional consistency across lifecycle changes;
- PostgreSQL-native range/exclusion constraints for effective-dated rules;
- JSONB flexibility without abandoning relational reporting/querying;
- one database engine across development, integration tests, and production-oriented deployments;
- ability to index frequently queried JSONB paths when needed.

Trade-offs:

- dynamic-field validation and metadata must be designed explicitly in the application;
- JSONB should not become a dumping ground for stable fields that deserve relational modelling;
- PostgreSQL-specific features reduce database portability;
- sensitive dynamic attributes may require field-level access/encryption controls rather than one unrestricted JSON document.

## Alternatives considered

### Fixed relational schema only

Rejected as the sole approach because tenant/jurisdiction-specific fields would force frequent schema changes and create many sparse columns.

### MongoDB/document database

Not selected because the central domain has strong relationships, historical/effective-dated rules, reporting needs, and transactional consistency requirements.

### Pure EAV

Rejected because it weakens typing, validation, query ergonomics, indexing, and reporting for data that should remain strongly modelled.
