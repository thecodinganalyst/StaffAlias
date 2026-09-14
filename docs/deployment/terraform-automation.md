# Terraform production automation

StaffAlias production Terraform uses a GCS remote backend and a protected GitHub Actions workflow after a one-time migration from the original local state.

## Ownership and safety

- `infrastructure/terraform` owns stable production infrastructure.
- The backend deployment workflow owns release-specific Cloud Run image, application environment variables, and secret bindings as documented by issue #42.
- Pull requests run Terraform format and validation only. They do not receive production GCP credentials.
- Pushes to `main` run an authenticated production plan once remote state is configured.
- Production apply is manual through `workflow_dispatch`, requires the `production` GitHub Environment, and only runs from `main` when the confirmation input is exactly `apply`.
- No service-account JSON key is used.

Pull-request planning is intentionally not authenticated against production. The existing production OIDC trust is restricted to `main` plus the `production` Environment; weakening that trust would allow pull-request workflow code to request production credentials. The authenticated plan therefore runs after merge on `main`.

## One-time bootstrap and migration

The repository originally used local Terraform state. Do not start GitHub Actions apply until the existing state has been migrated. The machine/Cloud Shell session that currently holds the authoritative `terraform.tfstate` must perform this one-time cutover.

### 1. Back up the authoritative state

From `infrastructure/terraform`:

```bash
cp terraform.tfstate terraform.tfstate.pre-gcs-backup
terraform state list > terraform-state-list.pre-gcs.txt
```

Keep the backup somewhere secure until the migration is verified. Never commit either file.

### 2. Bootstrap the Terraform automation identity with the existing local state

After pulling the code for issue #44, keep the existing local state active for one final apply:

```bash
rm -rf .terraform
terraform init -backend=false -input=false
terraform plan
terraform apply
```

Review the plan before approval. This creates `staffalias-terraform` and its production infrastructure-management IAM roles through the state that already owns the project resources.

### 3. Create the GCS state bucket

Bucket names are globally unique. A recommended pattern is:

```text
staffalias-terraform-state-<GCP_PROJECT_NUMBER>
```

Example commands:

```bash
export PROJECT_ID=staffalias
export REGION=asia-southeast1
export TF_STATE_BUCKET=staffalias-terraform-state-<GCP_PROJECT_NUMBER>

gcloud storage buckets create "gs://${TF_STATE_BUCKET}" \
  --project "${PROJECT_ID}" \
  --location "${REGION}" \
  --uniform-bucket-level-access

gcloud storage buckets update "gs://${TF_STATE_BUCKET}" --versioning

gcloud storage buckets add-iam-policy-binding "gs://${TF_STATE_BUCKET}" \
  --member="serviceAccount:staffalias-terraform@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin"
```

The bucket is deliberately bootstrapped outside the production root module because Terraform cannot use a GCS backend that does not exist yet.

### 4. Migrate local state to GCS

```bash
rm -rf .terraform
terraform init \
  -migrate-state \
  -force-copy \
  -backend-config="bucket=${TF_STATE_BUCKET}"
```

The backend prefix is fixed in `backend.tf` as `staffalias/production`.

### 5. Verify the migrated state

```bash
terraform state list > terraform-state-list.post-gcs.txt
diff -u terraform-state-list.pre-gcs.txt terraform-state-list.post-gcs.txt
terraform plan
```

The state lists should match. Review the plan and make sure there are no unexpected resource creates or destroys. In particular, Cloud Run must not be reverted to the bootstrap hello image and application DB secret bindings must not be removed.

### 6. Configure GitHub

In **Settings → Environments → production**, keep the existing variables:

- `GCP_PROJECT_ID`
- `GCP_REGION`
- `GCP_WORKLOAD_IDENTITY_PROVIDER`

Add:

```text
GCP_TERRAFORM_STATE_BUCKET=<the bucket name created above>
```

No new GitHub secret is required. The workflow derives the Terraform service-account email as:

```text
staffalias-terraform@<GCP_PROJECT_ID>.iam.gserviceaccount.com
```

Configure required reviewers/protection rules on the `production` Environment if approval before production changes is desired.

## Normal workflow after migration

### Pull requests

Terraform changes run:

```text
terraform fmt -check
terraform init -backend=false
terraform validate
```

No production credential is issued to pull-request code.

### Merge to main

If `GCP_TERRAFORM_STATE_BUCKET` is configured, the Terraform workflow authenticates by OIDC and runs a production `terraform plan` against the shared GCS state.

### Apply production changes

Go to **Actions → Terraform → Run workflow**, select `main`, and enter:

```text
apply
```

The workflow re-runs plan, saves the plan file, and applies that exact plan under the protected `production` Environment.

## Recovery

GCS object versioning is enabled so older state object generations can be recovered if required. Do not manually edit Terraform state in GCS. Prefer `terraform state` commands or restore a known-good GCS object generation only as an incident-recovery procedure.

Keep the pre-migration local backup until at least one successful GitHub Actions plan/apply cycle has completed. After migration, do not run independent local Terraform with a local state file; initialize against the GCS backend so all operators share the same state and locking behavior.
