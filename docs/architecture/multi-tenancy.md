# Multi-tenancy

StaffAlias uses a shared PostgreSQL database and shared schema. Tenant isolation is an application invariant enforced with `tenant_id`, tenant-scoped queries, database constraints, and automated isolation tests.

## Tenant identity

`tenant.id` is the immutable technical identifier used by foreign keys. `tenant.code` is the stable business/application identifier used to resolve a tenant. Business identifiers such as `employee_id` must never be used as tenant identifiers or foreign keys.

## Tenant context

Backend code must obtain the current tenant only through `TenantContext`. `ThreadLocalTenantContext` is the initial implementation. Tenant lookup by business code is centralised in `TenantResolutionService`.

Request controllers must not independently parse or resolve tenant IDs. When authentication is introduced, the authentication boundary will resolve the authenticated user's tenant claim/membership through `TenantResolutionService`, establish `TenantContext` before business processing, and clear it after the request. This keeps the domain independent of a particular identity provider.

No production API should trust an arbitrary client-supplied technical `tenant_id` as authority for tenant selection.

## Tenant-owned data

Every tenant-owned table must contain a non-null `tenant_id` foreign key to `tenant`. Every tenant-owned JPA aggregate must expose an explicit tenant association.

Repositories and services must query tenant-owned records using both the record identifier and the current tenant ID. Avoid unscoped methods such as `findById(id)` in business services for tenant-owned entities.

Example:

```java
settingRepository.findByIdAndTenant_Id(id, tenantContext.requireTenantId());
```

The initial `tenant_setting` aggregate demonstrates this pattern.

## Uniqueness

Values that are only unique within a company must include tenant identity in the database constraint. For example:

```text
UNIQUE (tenant_id, setting_key)
```

The same principle will apply to employee IDs and other company-specific codes. Global uniqueness should only be used for values that are truly global.

## Cross-tenant behavior

A record owned by another tenant is treated as not found. Services must not reveal whether another tenant owns the supplied technical ID. Read, update, and delete operations therefore use tenant-scoped lookup before acting.

Automated PostgreSQL integration tests must cover cross-tenant reads, updates, and deletes for tenant-owned aggregates.

## Rules for new features

1. Decide explicitly whether every new aggregate is global or tenant-owned.
2. Add non-null `tenant_id` to every tenant-owned table.
3. Use technical IDs for relationships; never use business identifiers as foreign keys.
4. Put `tenant_id` into tenant-local uniqueness constraints.
5. Scope repository methods by the current tenant.
6. Do not accept a tenant ID from request payloads as authorization.
7. Add isolation tests whenever a new tenant-owned aggregate is introduced.
8. Clear thread-local tenant context at the request boundary to prevent context leakage between requests.

## Future hardening

Application-level isolation is the first layer. PostgreSQL Row-Level Security may be evaluated later as defense in depth, but it does not replace correctly tenant-scoped application queries and tests.
