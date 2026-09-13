output "project_id" {
  description = "GCP project hosting StaffAlias production."
  value       = var.project_id
}

output "region" {
  description = "Production GCP region."
  value       = var.region
}

output "artifact_registry_repository" {
  description = "Artifact Registry repository ID."
  value       = google_artifact_registry_repository.backend.repository_id
}

output "artifact_registry_repository_url" {
  description = "Docker registry prefix used by deployment workflows."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.backend.repository_id}"
}

output "cloud_run_service_name" {
  description = "Cloud Run backend service name."
  value       = google_cloud_run_v2_service.backend.name
}

output "cloud_run_service_uri" {
  description = "Cloud Run backend service URI."
  value       = google_cloud_run_v2_service.backend.uri
}

output "runtime_service_account_email" {
  description = "Service account used by the running backend."
  value       = google_service_account.runtime.email
}

output "deployment_service_account_email" {
  description = "Service account intended for GitHub Actions deployment via Workload Identity Federation."
  value       = google_service_account.deployment.email
}

output "runtime_secret_ids" {
  description = "Secret Manager containers that require values before the real backend is deployed."
  value       = sort(tolist(local.runtime_secret_ids))
}
