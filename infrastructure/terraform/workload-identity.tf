resource "google_iam_workload_identity_pool" "github" {
  project                   = var.project_id
  workload_identity_pool_id = var.github_workload_identity_pool_id
  display_name              = "StaffAlias GitHub Actions"
  description               = "OIDC trust for production deployments from the StaffAlias repository"

  depends_on = [google_project_service.required]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  project                            = var.project_id
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = var.github_workload_identity_provider_id
  display_name                       = "StaffAlias GitHub"
  description                        = "Accepts GitHub Actions OIDC tokens only for StaffAlias production deployments"

  attribute_mapping = {
    "google.subject"                = "assertion.sub"
    "attribute.repository_id"       = "assertion.repository_id"
    "attribute.repository_owner_id" = "assertion.repository_owner_id"
    "attribute.ref"                 = "assertion.ref"
    "attribute.environment"         = "assertion.environment"
  }

  attribute_condition = join(" && ", [
    "assertion.repository_id == '${var.github_repository_id}'",
    "assertion.repository_owner_id == '${var.github_repository_owner_id}'",
    "assertion.ref == 'refs/heads/${var.github_deploy_branch}'",
    "assertion.environment == '${var.github_environment}'",
  ])

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com/"
  }
}

locals {
  github_repository_principal = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository_id/${var.github_repository_id}"
}

resource "google_service_account_iam_member" "github_deployment_workload_identity_user" {
  service_account_id = google_service_account.deployment.name
  role               = "roles/iam.workloadIdentityUser"
  member             = local.github_repository_principal
}

resource "google_service_account_iam_member" "github_terraform_workload_identity_user" {
  service_account_id = google_service_account.terraform.name
  role               = "roles/iam.workloadIdentityUser"
  member             = local.github_repository_principal
}
