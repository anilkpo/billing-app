# Billing App

Spring Boot fee tracking app with PostgreSQL storage and PDF bill generation.

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Data JPA
- PostgreSQL
- OpenHTMLToPDF (PDF generation)

## Run PostgreSQL (Local)

Create DB (example):

```sql
CREATE DATABASE billing_db;
```

Update credentials in `src/main/resources/application.properties` if needed.

## Run Application (Local)

```bash
mvn spring-boot:run
```

Application starts on `http://localhost:8080`.

## API Endpoints

1. Create fee entry

`POST /api/fees`

```json
{
  "studentName": "Anil Kumar",
  "studentEmail": "anil@example.com",
  "courseName": "Spring Boot",
  "amount": 25000,
  "paymentDate": "2026-05-11",
  "status": "PAID"
}
```

2. List all fee entries

`GET /api/fees`

3. Get one fee entry

`GET /api/fees/{id}`

4. Download PDF bill

`GET /api/fees/{id}/bill`

This returns a PDF file named `bill-{id}.pdf`.

## End-to-End Flow

1. User enters payment details via `POST /api/fees`.
2. Spring validates request data.
3. Fee record is stored in PostgreSQL table `fee_records`.
4. User calls `GET /api/fees/{id}/bill`.
5. Server fetches record, renders bill HTML, converts it to PDF, and streams it back.

## Docker Build

```bash
docker build -t billing-app:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/billing_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  billing-app:latest
```

## OpenShift Deployment (App + PostgreSQL)

1. Create/Open project

```bash
oc new-project billing
```

2. Deploy PostgreSQL

```bash
oc apply -f openshift/postgresql.yaml -n billing
```

3. Deploy billing app resources via template

```bash
oc process -f openshift/template.yaml \
  -p NAMESPACE=billing \
  -p GIT_REPO=https://github.com/<your-org>/<your-repo>.git \
  -p GIT_REF=master \
  -p DB_URL=jdbc:postgresql://postgresql.billing.svc.cluster.local:5432/billing_db \
  -p DB_USERNAME=postgres \
  -p DB_PASSWORD=postgres | oc apply -f -
```

4. Trigger app image build and verify rollout

```bash
oc start-build billing-app --follow -n billing
oc rollout status deployment/billing-app -n billing
oc get route billing-app -n billing
```

## One-Command OpenShift Deploy

Linux/macOS:

```bash
chmod +x scripts/deploy-openshift.sh
./scripts/deploy-openshift.sh --repo https://github.com/<your-org>/<your-repo>.git
```

Restricted cluster (no project-create permission):

```bash
./scripts/deploy-openshift.sh --repo https://github.com/<your-org>/<your-repo>.git --project <existing-project> --skip-project-create
```

Windows PowerShell:

```powershell
.\scripts\deploy-openshift.ps1 -Repo https://github.com/<your-org>/<your-repo>.git
```

Restricted cluster (no project-create permission):

```powershell
.\scripts\deploy-openshift.ps1 -Repo https://github.com/<your-org>/<your-repo>.git -Project <existing-project> -SkipProjectCreate
```

Both scripts support optional overrides:
- `project/app name`
- `git ref`
- `db host/port/name/user/password`
