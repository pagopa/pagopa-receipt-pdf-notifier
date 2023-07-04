prefix    = "pagopa"
env       = "uat"
env_short = "u"

tags = {
  CreatedBy   = "Terraform"
  Environment = "Uat"
  Owner       = "pagoPA"
  Source      = "https://github.com/pagopa/pagopa-receipt-pdf-notifier"
  CostCenter  = "TS310 - PAGAMENTI & SERVIZI"
}


github_repository_environment = {
  protected_branches     = false
  custom_branch_policies = true
  reviewers_teams        = ["pagopa-tech"]
}
