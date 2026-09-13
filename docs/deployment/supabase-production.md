# Supabase production database

StaffAlias uses Supabase PostgreSQL in production and Flyway for all schema changes. The production database must be reproducible from the version-controlled migrations under `backend/src/main/resources/db/migration`.

## Connection values

Configure the backend with these environment variables:

- `DB_URL` — JDBC URL for the Supabase PostgreSQL endpoint. For Cloud Run, prefer the Supabase transaction/session pooler endpoint appropriate for your workload. Include the SSL requirement in the JDBC URL, for example `?sslmode=require`.
- `DB_USERNAME` — database/pooler user supplied by Supabase.
- `DB_PASSWORD` — database password. Store this only in a secret store such as GCP Secret Manager.

Optional pool tuning variables:

- `DB_POOL_MAX_SIZE` — defaults to `5`
- `DB_POOL_MIN_IDLE` — defaults to `0`
- `DB_CONNECTION_TIMEOUT_MS` — defaults to `10000`
- `DB_VALIDATION_TIMEOUT_MS` — defaults to `5000`
- `DB_IDLE_TIMEOUT_MS` — defaults to `600000`
- `DB_MAX_LIFETIME_MS` — defaults to `1800000`
- `DB_KEEPALIVE_TIME_MS` — defaults to `0`

The conservative default pool size is intentional for Cloud Run because each instance owns its own JDBC pool. Increase it only after checking the Supabase connection limit and the configured Cloud Run maximum instance count.

## Example JDBC URL

Do not copy credentials into the repository. Build the actual value from the connection information shown in the Supabase dashboard.

```text
jdbc:postgresql://<supabase-pooler-host>:<port>/<database>?sslmode=require
```

If Supabase provides a pooler-specific username, use that exact value for `DB_USERNAME`.

## Secret handling

Issue #28 creates Secret Manager containers for the production datasource settings. Secret **values** must be added outside Terraform so they are not written into Terraform state.

Recommended mapping:

```text
staffalias-db-url      -> DB_URL
staffalias-db-username -> DB_USERNAME
staffalias-db-password -> DB_PASSWORD
```

The Cloud Run deployment in #31 should mount/reference these secrets as environment variables.

## Flyway migrations

Flyway is the only supported production schema-management mechanism. Do not make manual schema changes in Supabase unless performing a documented emergency recovery.

Migrations live in:

```text
backend/src/main/resources/db/migration/
```

On application startup with the `prod` profile, Flyway:

1. validates already-applied migrations,
2. applies any pending migrations in version order,
3. records the result in `flyway_schema_history`, and
4. refuses destructive `clean` operations because `clean-disabled` is enabled.

The integration test `PostgresIntegrationTest` starts a clean PostgreSQL Testcontainer and verifies that all migrations can be applied from scratch. CI runs this test as part of `mvn verify`.

## First production connection check

Before deploying the application, verify the values from a secure shell or one-off environment without printing the password:

```bash
export DB_URL='jdbc:postgresql://<host>:<port>/<database>?sslmode=require'
export DB_USERNAME='<username>'
export DB_PASSWORD='<password>'
```

Then start the backend with:

```bash
cd backend
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

A successful startup should show Flyway validation/migration completing and the application becoming ready.

## Migration failure and recovery

If a production migration fails:

1. stop the rollout or route traffic back to the previous healthy Cloud Run revision;
2. inspect the failed migration and `flyway_schema_history`;
3. determine whether PostgreSQL rolled the migration back completely;
4. fix the migration in a new versioned migration whenever possible rather than editing a migration that has already run successfully elsewhere;
5. if Flyway metadata is inconsistent after an interrupted/non-transactional operation, use `flyway repair` only after confirming the actual database state;
6. redeploy and verify the application and migration history before restoring normal rollout.

Never use `flyway clean` against production.

## Rollback considerations

Application rollback and database rollback are separate concerns. A previous Cloud Run revision can be restored quickly, but database migrations should normally be forward-compatible with at least the immediately previous application version. Prefer additive migrations, deploy application changes after compatible schema changes, and remove old columns/constraints only in a later release.

## Tenant isolation

The production database uses the same schema and tenant-key model as local/test PostgreSQL. Moving to Supabase does not replace application-level tenant isolation. Every tenant-scoped query and constraint must continue to enforce tenant ownership explicitly.
