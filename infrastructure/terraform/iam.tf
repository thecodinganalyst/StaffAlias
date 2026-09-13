resource "google_service_account" "runtime" {
  project      = var.project_id
  account_id   = "staffalias-runtime"
  display_name = "StaffAlias production runtime"

  depends_on = [google_project_service.required]
}

resource "google_service_account" "deployment" {
  project      = var.project_id
  account_id   = "staffalias-deploy"
  display_name = "StaffAlias GitHub deployment"

  depends_on = [google_project_service.required]
}

locals {
  deployment_project_roles = toset([
    "roles/artifactregistry.writer",
    "roles/run.admin",
  ])
}

resource "google_project_iam_member" "deployment_roles" {
  for_each = local.deployment_project_roles

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.deployment.email}"
}

resource "google_service_account_iam_member" "deployment_can_use_runtime" {
  service_account_id = google_service_account.runtime.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.deployment.email}"
}
