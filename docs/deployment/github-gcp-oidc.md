# GitHub Actions to GCP authentication

StaffAlias production deployments use GitHub Actions OpenID Connect (OIDC) with Google Cloud Workload Identity Federation. No long-lived GCP service-account JSON key is required.

## Trust boundary

Terraform creates a Workload Identity Pool and GitHub OIDC provider that only accepts tokens matching all of these conditions:

- GitHub repository ID: `1366973047` (`thecodinganalyst/StaffAlias`)
- GitHub repository owner ID: `60729103`
- branch: `main`
- GitHub Environment: `production`

The immutable numeric IDs are intentional. They avoid relying only on reusable repository or owner names.

The accepted external identity can impersonate only the `staffalias-deploy` service account. That service account retains the deployment permissions defined in Terraform.

## One-time GCP provisioning

Run Terraform using your own authenticated Google Cloud identity:

```bash
cd infrastructure/terraform
cp terraform.tfvars.example terraform.tfvars
# Set project_id to the real production GCP project ID.
terraform init
terraform plan
terraform apply
```

After apply, capture these outputs:

```bash
terraform output -raw github_workload_identity_provider
terraform output -raw deployment_service_account_email
terraform output -raw project_id
terraform output -raw region
terraform output -raw artifact_registry_repository
terraform output -raw cloud_run_service_name
```

## GitHub production environment

In GitHub, create an Environment named `production` under repository Settings > Environments.

Configure these Environment variables:

| Variable | Value |
| --- | --- |
| `GCP_PROJECT_ID` | Terraform `project_id` output |
| `GCP_REGION` | Terraform `region` output, normally `asia-southeast1` |
| `GCP_ARTIFACT_REGISTRY` | Terraform `artifact_registry_repository` output |
| `GCP_CLOUD_RUN_SERVICE` | Terraform `cloud_run_service_name` output |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Terraform `github_workload_identity_provider` output |
| `GCP_SERVICE_ACCOUNT` | Terraform `deployment_service_account_email` output |

These values are identifiers, not passwords, so they should be GitHub Environment variables rather than secrets.

Runtime application secrets such as the production database password should remain in GCP Secret Manager. Do not copy GCP service-account keys into GitHub Secrets.

## Recommended environment protection

Configure the `production` Environment so deployments require the controls you want, such as deployment branch restrictions and optional manual approval. The GCP provider independently enforces `main` and `production`, so changing only the GitHub workflow is not sufficient to bypass the GCP trust policy.

## Validate OIDC

After Terraform has been applied and the six environment variables above are configured, run the `GCP OIDC Smoke Test` workflow manually from the Actions tab on `main`.

The workflow:

1. requests a short-lived GitHub OIDC token,
2. exchanges it through Workload Identity Federation,
3. impersonates `staffalias-deploy`, and
4. reads the configured Cloud Run service.

A successful run proves that GitHub Actions can access GCP without a stored service-account key.

## Troubleshooting

If authentication is rejected, verify that the workflow is running from `main`, uses the `production` Environment, and that the GitHub repository/owner IDs in Terraform still match this repository. Also confirm that `GCP_WORKLOAD_IDENTITY_PROVIDER` is the fully qualified provider output, not only the provider ID.

If authentication succeeds but the Cloud Run describe step fails with a permission error, inspect the deployment service account IAM roles rather than broadening the Workload Identity provider trust.
