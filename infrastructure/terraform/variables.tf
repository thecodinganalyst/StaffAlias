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

variable "labels" {
  description = "Labels applied to supported resources."
  type        = map(string)
  default = {
    application = "staffalias"
    environment = "production"
    managed-by  = "terraform"
  }
}
