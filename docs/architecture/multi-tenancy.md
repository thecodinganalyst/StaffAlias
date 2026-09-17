# Multi-tenancy

StaffAlias uses a shared PostgreSQL database and shared schema. Tenant isolation is an application invariant enforced with `tenant_id`, tenant-scoped queries, database constraints, authenticated tenant context, and automated isolation tests.

## Tenant identity

`tenant.id` is the immutable technical identifier used by foreign keys. `tenant.code` is the stable business/application identifier used to resolve a tenant. Business identifiers such as `employee_id` must never be used as tenant identifiers or foreign keys.

## Authentication and roles

StaffAlias currently defines two administrative roles:

- `PLATFORM_ADMIN` is global to the StaffAlias platform and has no tenant association.
- `TENANT_ADMIN` belongs to exactly one tenant and operates only within that tenant.

Application accounts are stored in `application_user`. Passwords are stored only as password hashes and authentication is handled by Spring Security. The database also enforces the role/tenant invariant: a platform administrator cannot have a `tenant_id`, while a tenant administrator must have one.

The initial backend authentication mechanism is HTTP Basic so the authorization boundary can be established independently of the later frontend login/session flow. Authentication UX can evolve without changing the domain's tenant isolation model.

## Tenant provisioning

A `PLATFORM_ADMIN` provisions a new tenant and its first `TENANT_ADMIN` through one platform operation. Tenant creation and administrator creation run in the same database transaction, so a failed administrator creation cannot leave a tenant without its initial administrator.

The provisioning request supplies a temporary initial password. StaffAlias immediately hashes it with the configured `PasswordEncoder`; neither the plaintext password nor the hash is returned by the API or written to application logs. The initial password must be at least 12 characters. This is an interim bootstrap mechanism designed to be replaceable by a future invitation/password-reset flow without changing the tenant ownership model.

Tenant codes are globally unique. Application usernames are globally unique case-insensitively. Duplicate tenant codes or administrator usernames are rejected with HTTP 409 before provisioning completes.

## Tenant context

Backend code must obtain the current tenant only through `TenantContext`. `ThreadLocalTenantContext` is the initial implementation.

For authenticated `TENANT_ADMIN` requests, `TenantContextSecurityFilter` derives the tenant exclusively from the authenticated `StaffAliasPrincipal` and establishes `TenantContext` for the duration of the request. The filter clears the context in a `finally` block after request processing.

`PLATFORM_ADMIN` requests intentionally do not establish tenant context. Platform operations must use explicitly platform-scoped services rather than silently impersonating a tenant.

Request controllers must not independently parse or resolve tenant IDs. No production API may trust a tenant ID supplied in a request header, URL, query parameter, or payload as authority for tenant selection.

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

The same principle applies to employee IDs and other company-specific codes. Global uniqueness should only be used for values that are truly global. Application usernames are currently globally unique case-insensitively.

## Cross-tenant behavior

A record owned by another tenant is treated as not found. Services must not reveal whether another tenant owns the supplied technical ID. Read, update, and delete operations therefore use tenant-scoped lookup before acting.

Automated PostgreSQL integration tests must cover cross-tenant reads, updates, and deletes for tenant-owned aggregates. Security integration tests additionally verify authentication, role boundaries, and that manipulated client tenant headers do not change effective tenant scope.

## Rules for new features

1. Decide explicitly whether every new aggregate is global or tenant-owned.
2. Add non-null `tenant_id` to every tenant-owned table.
3. Use technical IDs for relationships; never use business identifiers as foreign keys.
4. Put `tenant_id` into tenant-local uniqueness constraints.
5. Scope repository methods by the authenticated current tenant.
6. Do not accept a tenant ID from request data as authorization.
7. Add isolation tests whenever a new tenant-owned aggregate is introduced.
8. Clear thread-local tenant context at the request boundary to prevent context leakage between requests.
9. Protect platform APIs with `PLATFORM_ADMIN` and tenant APIs with `TENANT_ADMIN`; hidden frontend routes are never a substitute for backend authorization.

## Future hardening

Application-level isolation is the first layer. PostgreSQL Row-Level Security may be evaluated later as defense in depth, but it does not replace correctly tenant-scoped application queries and tests.
