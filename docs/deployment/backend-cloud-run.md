# Backend deployment to Cloud Run

StaffAlias deploys the Spring Boot backend from GitHub Actions to Google Cloud Run using the OIDC trust configured in issue #30.

## Prerequisites

Before running the deployment workflow:

1. Terraform from `infrastructure/terraform` has been applied to the production GCP project.
2. GitHub Environment `production` contains:
   - `GCP_PROJECT_ID`
   - `GCP_REGION`
   - `GCP_ARTIFACT_REGISTRY`
   - `GCP_CLOUD_RUN_SERVICE`
   - `GCP_WORKLOAD_IDENTITY_PROVIDER`
   - `GCP_SERVICE_ACCOUNT`
3. GCP Secret Manager contains current versions for:
   - `staffalias-db-url`
   - `staffalias-db-username`
   - `staffalias-db-password`
   - `staffalias-platform-admin-password`
4. The runtime service account created by Terraform has Secret Manager access.

`GCP_PROJECT_ID` must be the textual project ID, not the numeric project number.

## Deployment workflow

Run **Deploy Backend to Cloud Run** from GitHub Actions on `main`.

The workflow performs these steps in order:

1. runs `mvn -B verify`, including the PostgreSQL/Flyway integration test;
2. authenticates to Google Cloud through Workload Identity Federation;
3. builds the backend Docker image;
4. pushes it to Artifact Registry using the immutable Git commit SHA as the image tag;
5. deploys that image to the configured Cloud Run service;
6. injects Secret Manager references for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`;
7. verifies `/actuator/health` until the service reports `UP`;
8. records the deployed image, Cloud Run revision, and service URL in the workflow summary.

No database secret values are copied into GitHub Actions.

## Runtime configuration

The production deployment currently applies:

| Setting | Value |
| --- | --- |
| Spring profile | `prod` |
| Container port | `8080` |
| CPU | `1` |
| Memory | `512Mi` |
| Minimum instances | `0` |
| Maximum instances | `3` |
| Concurrency | `80` |
| Request timeout | `300s` |
| Health endpoint | `/actuator/health` |

These defaults are intentionally conservative for the initial production rollout and can be adjusted as production usage becomes known.

## Database migrations

Flyway runs during application startup. Production enables migration validation and disables Flyway clean. A new revision is only considered healthy after application startup completes and `/actuator/health` reports `UP`.

If a migration fails, stop deployment work and follow the production database recovery procedure before attempting another revision. Do not manually alter `flyway_schema_history` unless the documented recovery procedure explicitly requires it.

## Verify the deployed service

The deployment workflow performs the health check automatically. You can also verify manually with:

```bash
SERVICE_URL="$(gcloud run services describe staffalias-api \
  --project YOUR_PROJECT_ID \
  --region asia-southeast1 \
  --format='value(status.url)')"

curl --fail --show-error "${SERVICE_URL}/actuator/health"
```

Expected response:

```json
{"status":"UP"}
```

## Roll back

Cloud Run keeps previous revisions. To restore traffic to an earlier healthy revision:

```bash
gcloud run revisions list \
  --service staffalias-api \
  --project YOUR_PROJECT_ID \
  --region asia-southeast1
```

Then route traffic to the chosen revision:

```bash
gcloud run services update-traffic staffalias-api \
  --project YOUR_PROJECT_ID \
  --region asia-southeast1 \
  --to-revisions REVISION_NAME=100
```

A rollback restores the application revision, but it does not reverse database migrations. Database changes must remain backward compatible with the revision being restored, or require a separately planned database recovery.

## Troubleshooting

- **OIDC authentication fails:** rerun the GCP OIDC smoke test and verify the `production` environment variables.
- **Image push fails:** verify the deployment service account still has Artifact Registry writer access.
- **Cloud Run deploy fails on service account permissions:** verify the deploy service account can act as the runtime service account.
- **Application fails during startup:** inspect Cloud Run revision logs, especially datasource or Flyway failures.
- **Health check fails:** inspect the latest Cloud Run revision logs and verify the three Secret Manager versions exist and contain the expected Supabase pooler connection values.
