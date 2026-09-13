resource "google_artifact_registry_repository" "backend" {
  project       = var.project_id
  location      = var.region
  repository_id = var.artifact_registry_repository
  description   = "StaffAlias production backend container images"
  format        = "DOCKER"
  labels        = var.labels

  depends_on = [google_project_service.required]
}
