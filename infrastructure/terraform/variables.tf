variable "project_id" {
  description = "Existing GCP project ID that hosts StaffAlias production resources."
  type        = string
}

variable "region" {
  description = "GCP region for regional resources."
  type        = string
  default     = "asia-southeast1"
}

variable "artifact_registry_repository" {
  description = "Artifact Registry repository ID for StaffAlias backend images."
  type        = string
  default     = "staffalias"
}

variable "cloud_run_service_name" {
  description = "Cloud Run service name for the StaffAlias backend."
  type        = string
  default     = "staffalias-api"
}

variable "cloud_run_image" {
  description = "Initial container image for the Cloud Run service. Issue #31 will replace this with the StaffAlias backend image."
  type        = string
  default     = "us-docker.pkg.dev/cloudrun/container/hello"
}

variable "cloud_run_container_port" {
  description = "Container port exposed by the Cloud Run service."
  type        = number
  default     = 8080
}

variable "cloud_run_cpu" {
  description = "CPU limit for the Cloud Run container."
  type        = string
  default     = "1"
}

variable "cloud_run_memory" {
  description = "Memory limit for the Cloud Run container."
  type        = string
  default     = "512Mi"
}

variable "cloud_run_min_instances" {
  description = "Minimum Cloud Run instance count."
  type        = number
  default     = 0
}

variable "cloud_run_max_instances" {
  description = "Maximum Cloud Run instance count."
  type        = number
  default     = 3
}

variable "allow_unauthenticated" {
  description = "Whether the Cloud Run endpoint is publicly invokable. Application-level authentication still applies."
  type        = bool
  default     = true
}

variable "github_repository_id" {
  description = "Immutable GitHub repository ID allowed to federate to GCP."
  type        = string
  default     = "1366973047"
}

variable "github_repository_owner_id" {
  description = "Immutable GitHub repository owner ID allowed to federate to GCP."
  type        = string
  default     = "60729103"
}

variable "github_deploy_branch" {
  description = "Only this GitHub branch may authenticate for production deployment."
  type        = string
  default     = "main"
}

variable "github_environment" {
  description = "GitHub Environment required for production deployment jobs."
  type        = string
  default     = "production"
}

variable "github_workload_identity_pool_id" {
  description = "GCP Workload Identity Pool ID used by GitHub Actions."
  type        = string
  default     = "github-actions"
}

variable "github_workload_identity_provider_id" {
  description = "OIDC provider ID inside the GitHub Actions Workload Identity Pool."
  type        = string
  default     = "staffalias"
}

variable "labels" {
  description = "Labels applied to supported resources."
  type        = map(string)
  default = {
    application = "staffalias"
    environment = "production"
    managed-by  = "terraform"
  }
}
