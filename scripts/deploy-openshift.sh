#!/usr/bin/env bash
set -euo pipefail

PROJECT="billing"
APP_NAME="billing-app"
GIT_REF="main"
DB_HOST="postgresql"
DB_PORT="5432"
DB_NAME="billing_db"
DB_USERNAME="postgres"
DB_PASSWORD="postgres"

usage() {
  cat <<EOF
Usage: $0 --repo <git_repo_url> [options]

Required:
  --repo         Git repository URL for app source

Optional:
  --project      OpenShift project/namespace (default: billing)
  --app-name     Application name in OpenShift (default: billing-app)
  --git-ref      Git branch/tag/commit (default: main)
  --db-host      PostgreSQL service host (default: postgresql)
  --db-port      PostgreSQL port (default: 5432)
  --db-name      PostgreSQL database name (default: billing_db)
  --db-user      PostgreSQL username (default: postgres)
  --db-password  PostgreSQL password (default: postgres)
EOF
}

GIT_REPO=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo) GIT_REPO="$2"; shift 2 ;;
    --project) PROJECT="$2"; shift 2 ;;
    --app-name) APP_NAME="$2"; shift 2 ;;
    --git-ref) GIT_REF="$2"; shift 2 ;;
    --db-host) DB_HOST="$2"; shift 2 ;;
    --db-port) DB_PORT="$2"; shift 2 ;;
    --db-name) DB_NAME="$2"; shift 2 ;;
    --db-user) DB_USERNAME="$2"; shift 2 ;;
    --db-password) DB_PASSWORD="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1"; usage; exit 1 ;;
  esac
done

if [[ -z "$GIT_REPO" ]]; then
  echo "Error: --repo is required"
  usage
  exit 1
fi

if ! command -v oc >/dev/null 2>&1; then
  echo "Error: oc CLI not found in PATH"
  exit 1
fi

DB_URL="jdbc:postgresql://${DB_HOST}.${PROJECT}.svc.cluster.local:${DB_PORT}/${DB_NAME}"

echo "[1/5] Ensuring project '${PROJECT}' exists"
oc get project "${PROJECT}" >/dev/null 2>&1 || oc new-project "${PROJECT}"
oc project "${PROJECT}" >/dev/null

echo "[2/5] Deploying PostgreSQL"
oc apply -f openshift/postgresql.yaml -n "${PROJECT}"
oc set env deployment/postgresql \
  POSTGRES_DB="${DB_NAME}" \
  POSTGRES_USER="${DB_USERNAME}" \
  POSTGRES_PASSWORD="${DB_PASSWORD}" -n "${PROJECT}"
oc patch secret postgres-secret -n "${PROJECT}" --type merge -p "{\"stringData\":{\"POSTGRES_DB\":\"${DB_NAME}\",\"POSTGRES_USER\":\"${DB_USERNAME}\",\"POSTGRES_PASSWORD\":\"${DB_PASSWORD}\"}}"

echo "[3/5] Deploying app resources"
oc process -f openshift/template.yaml \
  -p NAMESPACE="${PROJECT}" \
  -p APP_NAME="${APP_NAME}" \
  -p GIT_REPO="${GIT_REPO}" \
  -p GIT_REF="${GIT_REF}" \
  -p DB_URL="${DB_URL}" \
  -p DB_USERNAME="${DB_USERNAME}" \
  -p DB_PASSWORD="${DB_PASSWORD}" | oc apply -f -

echo "[4/5] Starting build"
oc start-build "${APP_NAME}" --follow -n "${PROJECT}"

echo "[5/5] Waiting for rollout and printing route"
oc rollout status deployment/"${APP_NAME}" -n "${PROJECT}"
oc get route "${APP_NAME}" -n "${PROJECT}"

echo "Deployment completed."
