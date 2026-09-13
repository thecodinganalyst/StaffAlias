# Architecture Decision Records

Architecture Decision Records (ADRs) capture significant technical decisions that affect StaffAlias beyond one implementation detail.

## When to create an ADR

Create or update an ADR when a decision changes architecture, persistence strategy, security/tenancy boundaries, deployment model, major framework choice, or another cross-cutting engineering constraint.

## Format

Each ADR should contain:

- title and status;
- context/problem;
- decision;
- consequences and trade-offs;
- alternatives considered where useful.

ADRs are numbered sequentially and should not be silently rewritten after the decision materially changes. Supersede an older ADR with a newer one when appropriate.

## Records

- [0001 — PostgreSQL with relational tables and JSONB](0001-postgresql-and-jsonb.md)
