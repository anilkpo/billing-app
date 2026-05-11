param(
  [Parameter(Mandatory = $true)]
  [string]$Repo,
  [string]$Project = "billing",
  [string]$AppName = "billing-app",
  [string]$GitRef = "master",
  [string]$DbHost = "postgresql",
  [string]$DbPort = "5432",
  [string]$DbName = "billing_db",
  [string]$DbUser = "postgres",
  [string]$DbPassword = "postgres",
  [switch]$SkipProjectCreate
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command oc -ErrorAction SilentlyContinue)) {
  throw "oc CLI not found in PATH"
}

$dbUrl = "jdbc:postgresql://$DbHost.$Project.svc.cluster.local:$DbPort/$DbName"

Write-Host "[1/5] Ensuring project '$Project' exists"
$projectExists = $true
try {
  oc get project $Project | Out-Null
} catch {
  $projectExists = $false
}
if (-not $projectExists) {
  if ($SkipProjectCreate) {
    throw "Project '$Project' does not exist and -SkipProjectCreate was set. Ask your OpenShift admin to create it, then rerun."
  }
  oc new-project $Project | Out-Null
}
oc project $Project | Out-Null

Write-Host "[2/5] Deploying PostgreSQL"
oc apply -f openshift/postgresql.yaml -n $Project | Out-Null
oc set env deployment/postgresql POSTGRES_DB=$DbName POSTGRES_USER=$DbUser POSTGRES_PASSWORD=$DbPassword -n $Project | Out-Null
oc patch secret postgres-secret -n $Project --type merge -p "{\"stringData\":{\"POSTGRES_DB\":\"$DbName\",\"POSTGRES_USER\":\"$DbUser\",\"POSTGRES_PASSWORD\":\"$DbPassword\"}}" | Out-Null

Write-Host "[3/5] Deploying app resources"
oc process -f openshift/template.yaml `
  -p NAMESPACE=$Project `
  -p APP_NAME=$AppName `
  -p GIT_REPO=$Repo `
  -p GIT_REF=$GitRef `
  -p DB_URL=$dbUrl `
  -p DB_USERNAME=$DbUser `
  -p DB_PASSWORD=$DbPassword | oc apply -f - | Out-Null

Write-Host "[4/5] Starting build"
oc start-build $AppName --follow -n $Project

Write-Host "[5/5] Waiting for rollout and printing route"
oc rollout status deployment/$AppName -n $Project
oc get route $AppName -n $Project

Write-Host "Deployment completed."
