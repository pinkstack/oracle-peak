provider "google" {
  credentials = file("./ob-service_account.json")
  project = "oracle-peak"
  region = "europe-west3"
}