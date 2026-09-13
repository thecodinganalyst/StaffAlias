# Staff lifecycle domain

## Core identity model

StaffAlias separates the stable human identity from individual employment relationships:

```text
Tenant
  └── Person
        └── Employment
              └── EmploymentIdentifier (EMPLOYEE_ID)
```

`Person.id`, `Employment.id`, and `EmploymentIdentifier.id` are technical UUID primary keys. Business identifiers such as `employee_id` are never used as foreign keys.

## Rehire behavior

A person may have multiple employment records. A later employment may reuse a previous `employee_id` or receive a different one. Historical employment rows are retained rather than overwritten.

Employee IDs are represented by effective-dated `employment_identifier` rows. This also leaves room for an employee ID to change during one employment without discarding identifier history.

## Tenant rules

`person`, `employment`, and `employment_identifier` are tenant-owned. Their services resolve data through the current `TenantContext`, and database foreign keys include tenant ownership where an aggregate references another tenant-owned aggregate.

Employee IDs are not globally unique. The same value can be used by different tenants.

## Employee ID overlap rule

Within one tenant, the same `employee_id` cannot be assigned to overlapping effective periods. PostgreSQL enforces this with a GiST exclusion constraint over:

- `tenant_id`
- `employee_id`
- the effective date range

This permits an ID to be reused after the previous assignment has ended while preventing simultaneous conflicting use.

## Future extensions

Assignment history, dynamic employment data, and lifecycle events should reference the technical employment ID and follow the same tenant-scoping and effective-dating rules.
