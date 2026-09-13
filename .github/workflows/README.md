# GitHub Actions workflows

`ci.yml` is the required pull-request and `main` branch verification workflow for StaffAlias.

It intentionally keeps backend and frontend checks in separate jobs so failures are easy to diagnose. Backend verification includes PostgreSQL Testcontainers and Flyway clean-database migration coverage; frontend verification includes install, lint, typecheck, tests, and production build.
