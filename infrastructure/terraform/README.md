# StaffAlias production infrastructure

This directory contains Terraform for the existing StaffAlias production GCP project. It intentionally **does not create the GCP project or billing account**; `project_id` is an input because the project has already been created.

## What Terraform creates

- Required Google Cloud APIs
- Docker Artifact Registry repository
- Cloud Run v2 backend service skeleton
- Dedicated runtime and deployment service accounts
- Least-privilege deployment IAM needed for later GitHub Actions deployment
- Secret Manager containers for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`

Terraform creates only the secret containers. **Do not put real secret values in `terraform.tfvars`, Terraform resources, or source control.** Secret values are added separately and consumed by the production deployment in later issues.

## Prerequisites

1. An existing GCP project with billing enabled.
2. Terraform >= 1.7.
3. Google Cloud CLI authenticated as a principal that can enable services and create the resources/IAM bindings in this module.
4. Application Default Credentials for Terraform:

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project YOUR_GCP_PROJECT_ID
```

If the APIs required to enable other services are not already usable by your account/project, bootstrap Service Usage first with:

```bash
gcloud services enable serviceusage.googleapis.com --project YOUR_GCP_PROJECT_ID
```

## Configure

Copy the example file and edit only non-secret infrastructure values:

```bash
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars
```

At minimum set:

```hcl
project_id = "your-real-gcp-project-id"
region     = "asia-southeast1"
```

`asia-southeast1` is the recommended default for a Singapore deployment but remains configurable.

## Validate and plan

```bash
terraform fmt -check
terraform init
terraform validate
terraform plan
```

Review the plan before applying it. In particular, verify the GCP project ID and region.

## Apply

```bash
terraform apply
```

Useful outputs include the Artifact Registry URL, Cloud Run service URI, runtime service account, and deployment service account:

```bash
terraform output
```

## Add production database secret values

Issue #29 defines the exact Supabase production database configuration. Once those values are known, add **secret versions** outside Terraform so their plaintext values do not enter Terraform state. For example:

```bash
printf '%s' "$DB_URL" | gcloud secrets versions add staffalias-db-url --data-file=- --project YOUR_GCP_PROJECT_ID
printf '%s' "$DB_USERNAME" | gcloud secrets versions add staffalias-db-username --data-file=- --project YOUR_GCP_PROJECT_ID
printf '%s' "$DB_PASSWORD" | gcloud secrets versions add staffalias-db-password --data-file=- --project YOUR_GCP_PROJECT_ID
```

Do not paste production credentials into shell history if your environment records commands; using a secure local environment variable or secret-management workflow is preferred.

## Cloud Run bootstrap image

The Cloud Run service initially uses Google's public Cloud Run hello container. This allows infrastructure provisioning to complete before the StaffAlias production backend image exists. Issue #31 replaces this placeholder with an immutable StaffAlias image from the provisioned Artifact Registry repository.

The service is publicly invokable by default because the browser frontend must reach the API; authentication/authorization remains an application responsibility. Set `allow_unauthenticated = false` if the intended architecture changes.

## IAM design

- `staffalias-runtime`: identity used by the running backend. It can access only the Secret Manager secrets created by this module.
- `staffalias-deploy`: identity reserved for GitHub Actions. It can push Artifact Registry images, administer Cloud Run deployment, and act as the runtime service account when deploying revisions.
- Issue #30 will add Workload Identity Federation so GitHub Actions can impersonate the deployment account without a long-lived JSON service-account key.

## State

This first iteration uses normal local Terraform state. Never commit `.tfstate` files. Before multiple operators or automated Terraform applies are introduced, migrate state to a protected remote backend such as a dedicated GCS bucket with versioning and appropriate access controls.

## Teardown caution

`google_project_service` resources use `disable_on_destroy = false`, so destroying this module will not disable GCP APIs that other project resources may still depend on. Review any production destroy plan carefully before approving it.
