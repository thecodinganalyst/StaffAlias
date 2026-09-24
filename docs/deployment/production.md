# Production deployment and release verification

This runbook describes the controlled production release path for StaffAlias. Production releases are started manually from GitHub Actions and use the GitHub `production` environment as the approval/protection boundary.

## Prerequisites

Before the first release, complete the infrastructure and platform setup described in:

- `docs/deployment/terraform-automation.md`
- `docs/deployment/github-gcp-oidc.md`
- `docs/deployment/supabase-production.md`
- `docs/deployment/backend-cloud-run.md`
- `docs/deployment/frontend.md`

The production infrastructure must provide a Cloud Run service, Artifact Registry repository, Firebase Hosting site, GitHub-to-GCP Workload Identity Federation, and a reachable Supabase PostgreSQL database. Cloud Run must grant `allUsers` `roles/run.invoker` so Firebase Hosting can invoke the `/api/**` rewrite; StaffAlias/Spring Security still authenticates and authorizes protected application routes.

## First-time infrastructure provisioning

1. Configure the Terraform remote state bucket and the GitHub variables required by `.github/workflows/terraform.yml`.
2. Run the Terraform workflow in plan mode and review the proposed resources.
3. Run the Terraform workflow with apply enabled after approval.
4. Confirm the Artifact Registry repository and Cloud Run service exist in the configured GCP project/region.
5. Create/configure the Firebase Hosting site referenced by `FIREBASE_HOSTING_SITE`.
6. Complete the Supabase production setup and load the database credentials into Google Secret Manager as documented below.
7. Validate GitHub OIDC by running the dedicated GCP OIDC smoke workflow before the first application deployment.

## Required GitHub environment variables

Configure these variables on the GitHub `production` environment:

| Variable | Purpose |
| --- | --- |
| `GCP_PROJECT_ID` | Production GCP project ID |
| `GCP_REGION` | Cloud Run and Artifact Registry region |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Workload Identity Provider resource name |
| `GCP_SERVICE_ACCOUNT` | GitHub Actions deployment service account |
| `GCP_ARTIFACT_REGISTRY` | Artifact Registry repository name |
| `GCP_CLOUD_RUN_SERVICE` | Production Cloud Run service name |
| `FIREBASE_HOSTING_SITE` | Firebase Hosting site ID |
| `PLATFORM_ADMIN_USERNAME` | Initial Platform Admin username (use `platformadmin` in production) |

The GitHub `production` environment should require reviewer approval. This keeps production deployment explicit even though the workflow itself is manually dispatched.

## Required production secrets

Backend deployment reads the following Google Secret Manager secrets and maps them into the Cloud Run container:

- `staffalias-db-url` -> `DB_URL`
- `staffalias-db-username` -> `DB_USERNAME`
- `staffalias-db-password` -> `DB_PASSWORD`
- `staffalias-platform-admin-password` -> `PLATFORM_ADMIN_PASSWORD`

Do not store database or Platform Admin passwords directly in the repository or workflow YAML.

### Platform Admin production bootstrap

Set the GitHub production environment variable `PLATFORM_ADMIN_USERNAME` to `platformadmin`. Terraform creates the Google Secret Manager container `staffalias-platform-admin-password` and grants the runtime service account access. After Terraform apply, add a secret version containing the initial password; do not manage the secret value in Terraform. The backend deployment maps these values to `PLATFORM_ADMIN_USERNAME` and `PLATFORM_ADMIN_PASSWORD`.

Both values are required together. On the first startup, StaffAlias creates the platform-scoped `PLATFORM_ADMIN` account and stores only its BCrypt password hash. On subsequent deployments, bootstrap detects the existing account and leaves its stored password unchanged; changing the Secret Manager value alone therefore does not reset an existing account.

Do not print, echo, or expose the plaintext password in GitHub Actions, Cloud Run logs, API responses, frontend configuration, or source control.

## Supabase production configuration

Use a production Supabase PostgreSQL database that is separate from development data. Configure the JDBC URL, username, and password in the three Google Secret Manager entries above. Flyway runs automatically when the backend starts with the `prod` Spring profile. `clean` is disabled in production and migration validation is enabled.

Before deploying a migration that is difficult to reverse, take an appropriate Supabase backup/snapshot and review the migration for backward compatibility with the currently deployed application.

## Normal release procedure

1. Merge the intended release commit to `main` and confirm normal CI is green.
2. Open **Actions -> Production Release -> Run workflow** and select `main`.
3. Approve the `production` environment deployment when prompted.
4. The release workflow calls the existing CI workflow. Backend and frontend CI must pass before deployment starts.
5. The backend workflow builds an immutable image tagged with the Git commit SHA, pushes it to Artifact Registry, and deploys it to Cloud Run.
6. The frontend workflow builds the frontend with the same-origin API base and deploys it to Firebase Hosting with the Git SHA in the deployment message.
7. The final verification job checks direct Cloud Run health, Firebase availability, the read-only database-backed API through Firebase's same-origin `/api/**` proxy, and a real Platform Admin session login followed by `/api/auth/me`.
8. Treat a failed verification job as a failed release. Investigate or roll back before considering the release complete.

Production verification never creates or changes business data. `/api/system/database-readiness` performs only `SELECT 1` and returns `{"status":"UP"}` when the application can query the production database. The authentication smoke test uses the existing Platform Admin credential from Secret Manager, keeps the session cookie in a temporary cookie jar, calls only `/api/auth/me`, and never prints the password.

## Identifying the deployed version

The backend image is tagged with the Git commit SHA:

```text
<region>-docker.pkg.dev/<project>/<repository>/backend:<git-sha>
```

The backend deployment summary records the image URI, Cloud Run revision, service URL, and source commit. The final production release summary records the same source commit and active Cloud Run revision.

Firebase deployment uses the message `GitHub <git-sha>`, so the Hosting release can also be correlated with the source commit.

To inspect the active Cloud Run revision manually:

```bash
gcloud run services describe "$GCP_CLOUD_RUN_SERVICE" \
  --project "$GCP_PROJECT_ID" \
  --region "$GCP_REGION" \
  --format='value(status.latestReadyRevisionName)'
```

## Cloud Run rollback

Cloud Run keeps prior revisions. To restore a known-good backend revision without rebuilding it:

1. List revisions and identify the last known-good revision.
2. Move all traffic to that revision.
3. Re-run the production smoke checks against the service URL.

Example:

```bash
gcloud run revisions list \
  --service "$GCP_CLOUD_RUN_SERVICE" \
  --project "$GCP_PROJECT_ID" \
  --region "$GCP_REGION"

gcloud run services update-traffic "$GCP_CLOUD_RUN_SERVICE" \
  --project "$GCP_PROJECT_ID" \
  --region "$GCP_REGION" \
  --to-revisions <known-good-revision>=100
```

Rollback is safe only when the older application is compatible with the current database schema. Prefer backward-compatible database migrations for this reason.

## Frontend rollback

Firebase Hosting retains release history. Use the Firebase console Hosting release history or Firebase CLI to identify the prior known-good release and roll back to it. After rollback, verify the Hosting URL and confirm that its backend API configuration is compatible with the active Cloud Run revision.

If a rollback command is performed outside GitHub Actions, record the affected release/revision and commit in the incident or deployment notes so traceability is preserved.

## Database migration failure and recovery

If Flyway fails during backend startup:

1. The new Cloud Run revision should not become healthy; do not direct traffic to it.
2. Inspect Cloud Run logs and the Flyway error before making any database change.
3. If the migration made no schema changes, fix the migration/application and deploy a new commit.
4. If the migration partially changed the schema, restore from the approved Supabase backup or apply a reviewed corrective migration. Avoid manually editing Flyway history unless the database state has been independently verified.
5. Keep traffic on the last known-good Cloud Run revision while recovery is in progress when schema compatibility allows it.
6. Re-run the full production release workflow after recovery.

Never use `flyway clean` against production.

## Release verification details

The final job verifies the same path used by production browsers:

- The Cloud Run IAM policy must contain `allUsers -> roles/run.invoker`; the workflow fails explicitly if this Terraform-managed policy has drifted.
- Direct `GET /actuator/health` confirms the Cloud Run service is serving a healthy revision.
- The Firebase Hosting production URL must return HTTP success.
- `GET https://<site>.web.app/api/system/database-readiness` must pass through the Hosting rewrite, execute `SELECT 1`, and return `status=UP`.
- A same-origin `POST /api/auth/login` followed by `GET /api/auth/me` with the returned session cookie must identify the configured account as `PLATFORM_ADMIN`.

Verification retries bounded transient failures such as network errors, timeouts, and server-side 5xx responses. Authentication/authorization and other 4xx responses fail immediately because waiting cannot correct a deterministic client or security configuration error. Database-readiness failures print the safe response and recent Cloud Run logs to make release failures actionable.

The previous direct frontend-to-Cloud-Run CORS verification is intentionally removed. Production browser traffic now uses the Firebase Hosting `/api/**` same-origin proxy.

## Basic troubleshooting

### CI fails before deployment

Fix the failing backend or frontend CI check first. The release workflow intentionally does not bypass CI.

### GitHub OIDC authentication fails

Verify the production environment variables, Workload Identity Provider attribute conditions, service-account IAM bindings, and repository/ref restrictions. Run `.github/workflows/gcp-oidc-smoke.yml` to isolate authentication from deployment.

### Firebase API rewrite returns 401

Check the Cloud Run IAM policy first. Terraform manages `allUsers` with `roles/run.invoker` when `allow_unauthenticated=true`. This permission only lets requests reach the application; Spring Security continues to protect authenticated StaffAlias endpoints. Run Terraform apply if the IAM binding has drifted.

The deployment service account has `roles/logging.viewer` so release diagnostics can read recent Cloud Run logs. If diagnostics report `PERMISSION_DENIED`, apply the current Terraform IAM configuration.

### Cloud Run starts but health verification fails

Check Cloud Run revision logs, Spring `prod` profile activation, Secret Manager access, database connectivity, Flyway migration output, container port 8080, and the configured service resource limits.

### Database readiness fails

Confirm the three database secrets point to the production Supabase database, the Supabase project is available, network access is allowed, and the database user can connect and execute `SELECT 1`.

### Frontend works but API calls fail in the browser

Confirm browser requests target the Firebase Hosting origin under `/api/**`, not the direct `run.app` URL. Check the Firebase rewrite order and re-run the release workflow after correcting configuration.

### Firebase deployment fails

Verify `FIREBASE_HOSTING_SITE`, project access for the GitHub deployment identity, and the Hosting site configuration. The workflow validates the configured site before deployment.

## Rollback validation

The rollback procedures rely on provider-supported immutable Cloud Run revisions and Firebase Hosting release history rather than rebuilding old source. Before the first high-impact production release, perform a controlled rollback rehearsal in production during a maintenance window or in an equivalent non-production environment: deploy two harmless revisions, switch Cloud Run traffic back to the earlier revision, restore the earlier Hosting release, and run the same smoke checks. Record the tested revision/release IDs and outcome in the release notes.
