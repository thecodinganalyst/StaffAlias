# Data model

StaffAlias separates stable human identity from employment relationships and from business-facing identifiers.

## Core model

```text
Tenant
  └── Person
        └── Employment
              └── EmploymentIdentifier (EMPLOYEE_ID)
```

### Person

`Person` is the stable identity for one human within a tenant. It uses an immutable technical UUID primary key. Rehire does not create a new person merely because the employment relationship changes.

### Employment

`Employment` represents one effective employment relationship for a person. A person can therefore have multiple employment records across time. Employment has its own immutable technical UUID and effective start/end dates.

### EmploymentIdentifier

The employee-facing identifier is called `employee_id`. It is stored as an effective-dated employment identifier rather than as a foreign key.

This supports all of the following without losing history:

- rehire using the same employee ID;
- rehire using a different employee ID;
- later employee-ID changes during one employment;
- the same employee ID value being used independently in different tenants.

Within one tenant, overlapping effective periods for the same `employee_id` are prevented by a PostgreSQL exclusion constraint.

## Effective dating

Where a value can change while historical truth still matters, StaffAlias should prefer effective-dated records with `effective_from` and `effective_to` instead of overwriting prior values.

This pattern will later apply to areas such as assignment history, manager, organisation, location, jurisdiction, and other employment attributes.

## Planned dynamic fields

Different jurisdictions and companies require different staff fields. Stable, frequently queried concepts belong in relational columns/tables. Configurable attributes are expected to use metadata-driven definitions plus PostgreSQL JSONB where appropriate.

Planned concepts include:

- field definitions with code, label, data type, scope, validation, and display metadata;
- tenant/jurisdiction-specific field rules;
- effective-dated dynamic values;
- field-level handling for sensitive data.

The design intentionally avoids a pure entity-attribute-value model for all staff data because that would weaken typing, querying, reporting, and referential integrity.

## Planned lifecycle events

Lifecycle events will become first-class records for actions such as hire, rehire, transfer, promotion, manager change, location change, employment-type change, leave of absence, resignation, termination, and retirement.

The intent is to preserve both effective state and the business event that caused a change.

For the implemented model, see [staff-lifecycle-domain.md](staff-lifecycle-domain.md).
