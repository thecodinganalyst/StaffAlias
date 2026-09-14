terraform {
  backend "gcs" {
    prefix = "staffalias/production"
  }
}
