# Product Verification System

A full-stack system for bulk product ingestion, floor-level product verification, and
compliance reporting in a warehouse setting.

Built for: Bulk CSV import (millions of rows) → barcode-driven floor validation with photo
capture → date-range verification reports. Includes optional RBAC (Admin / Operator).

\---

## 1\. Tech Stack

|Layer|Choice|Why|
|-|-|-|
|Backend|Java 17, Spring Boot 3.2|Matches production stack; mature ecosystem for batch/async work|
|Database|PostgreSQL 16|Strong relational guarantees for a strict-uniqueness primary key (WID), fast range queries with indexes, native `COPY`/bulk-friendly, `ON CONFLICT` upserts|
|Cache|Redis 7|Caches repeated report queries (QA managers re-run the same date range often)|
|Migrations|Flyway|Schema is version-controlled, not left to Hibernate auto-DDL|
|Auth|Spring Security + JWT|Stateless, horizontally scalable, no server-side session store needed|
|Frontend|React 18 + Redux Toolkit + Vite|Matches production stack; RTK for predictable auth/session state|
|Bulk CSV parsing|OpenCSV (streaming)|Never loads the full file into memory|
|Containerization|Docker Compose|One-command spin-up of Postgres + Redis + backend + frontend|

\---

## 2\. Architecture Decisions (for the walkthrough)

### 2.1 Bulk CSV ingestion at scale (millions of rows)

The naive approach — read the whole file, `INSERT`/upsert row by row — falls over past a few
hundred thousand rows (memory pressure, round-trip latency, lock contention). Instead:

1. **Request returns immediately.** The controller streams the multipart upload straight to a
temp file on disk and returns `202 Accepted` with a `jobId`. It does not block on parsing.
2. **Async streaming parse.** A background thread (`@Async`, dedicated thread pool) reads the
CSV row-by-row with OpenCSV — constant memory regardless of file size — and JDBC-batches
rows (default batch size 5,000) into an unlogged `staging\\\_products` table tagged with the
job id.
3. **Set-based merge.** Once staging is loaded, a single SQL statement (`INSERT ... SELECT ... ON CONFLICT (wid) DO UPDATE`) merges staging into `products`. This is one round trip
for the whole file instead of one round trip per row, and it's where WID uniqueness is
enforced — at the database constraint level, not in application code, so it holds even
under concurrent uploads.
4. **Progress polling.** The frontend polls `GET /api/upload/status/{jobId}` every 1.5s and
shows a live progress bar (rows processed / total, inserted vs. updated vs. failed) instead
of holding a spinner against a single long HTTP call.
5. **Bad rows don't fail the job.** A row with a missing WID or unparseable date is skipped
and counted in `failed\\\_rows`; the rest of the file still loads.

This is deliberately not Kafka-based: for a single bulk-import endpoint, a staging table +
set-based upsert is simpler to operate and just as scalable. If ingestion needed to fan out to
multiple downstream consumers (e.g., notify a separate compliance system per row), a
Kafka Outbox pattern would be the next step — happy to discuss that trade-off live.

### 2.2 Floor validation usability

* WID input is a plain autofocused text field: USB/Bluetooth barcode scanners emit keystrokes

  * Enter, so no special scanner integration is needed — the browser just sees fast typing.
* Camera capture uses `<input type="file" accept="image/\\\*" capture="environment">`, which
opens the native camera directly on a handheld/mobile device without extra libraries.
* After each verification, the form resets and refocuses the WID field automatically so an
operator can keep scanning items back-to-back without touching anything but the scanner and
a "next" tap.

### 2.3 Data integrity

* `products.wid` is the primary key — the database itself rejects a duplicate WID, not just
application logic.
* The staging→products merge de-duplicates in-file duplicate WIDs (`DISTINCT ON (wid) ... ORDER BY seq\\\_no DESC`, so the last row for a WID in the file wins) before the upsert, since
`ON CONFLICT` can't affect the same row twice in one statement.
* Every validation event is immutably logged (`validation\\\_logs`) with a snapshot of the EAN/
dates at verification time, the operator, and a timestamp — so reports reflect what was
actually shown to the operator even if the product record is edited later.

### 2.4 Reporting performance

* `validation\\\_logs.verified\\\_at` is indexed, so date-range scans stay fast as the table grows
into the tens of millions of rows.
* Report pages are capped at 500 rows and paginated — a client can't accidentally request a
huge in-memory payload.
* Repeated queries for the same date range are cached in Redis for 2 minutes.

\---

## 3\. Data Model

```
products                         validation\\\_logs                upload\\\_jobs
---------                        ----------------                -----------
wid (PK)                         id (PK)                         id (PK, uuid)
ean            \\\[indexed]         wid              \\\[indexed]      file\\\_name
manufacturing\\\_date                ean                             status
expiry\\\_date                      manufacturing\\\_date               total/processed/
created\\\_at                       expiry\\\_date                        inserted/updated/
updated\\\_at                       found                               failed\\\_rows
                                  operator\\\_username                error\\\_message
                                  image\\\_path                       created\\\_by / at
                                  verified\\\_at      \\\[indexed]        updated\\\_at

users                             staging\\\_products (transient, per-job)
-----                             ------------------------------------
id (PK)                          job\\\_id, wid, ean, manufacturing\\\_date,
username (unique)                expiry\\\_date, seq\\\_no
password\\\_hash
role (ADMIN | OPERATOR)
enabled
```

\---

## 4\. Setup \& Deployment

### Option A — Docker Compose (recommended, one command)

Requires Docker + Docker Compose installed.

```bash
cd pvs
docker compose up --build
```

This starts Postgres, Redis, the backend (port `8080`), and the frontend (port `3000`).
Flyway runs migrations automatically on backend startup, including seed users:

|Username|Password|Role|
|-|-|-|
|`admin`|`admin123`|ADMIN|
|`operator`|`operator123`|OPERATOR|

**Change these before any real deployment** — create new users from the User Management page
and disable/rotate the seeded ones.

Open **http://localhost:3000**.

### Option B — Manual local development

**Backend**

```bash
cd backend
# start Postgres and Redis yourself, e.g.:
docker run -d --name pg -e POSTGRES\\\_DB=pvs -e POSTGRES\\\_USER=pvs\\\_user \\\\
  -e POSTGRES\\\_PASSWORD=pvs\\\_password -p 5432:5432 postgres:16-alpine
docker run -d --name redis -p 6379:6379 redis:7-alpine

mvn spring-boot:run
```

Backend runs on `http://localhost:8080`. Flyway migrates the schema on startup.

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` (Vite dev server), talking to the backend via
`VITE\\\_API\\\_BASE\\\_URL` (defaults to `http://localhost:8080`, override in a `.env` file).

### Testing bulk ingestion at scale

A generator script is included to produce large CSVs for a live scale demo:

```bash
cd scripts
python3 generate\\\_sample\\\_csv.py --rows 2000000 --out sample\\\_products.csv
```

Upload `sample\\\_products.csv` from the Bulk Upload page (as `admin`) and watch the job
progress bar. On a laptop-class machine this comfortably handles multi-million-row files
without the request timing out, because ingestion is fully async.

\---

## 5\. API Reference

|Method|Path|Role|Description|
|-|-|-|-|
|POST|`/api/auth/login`|—|Returns a JWT|
|POST|`/api/users`|ADMIN|Create a user|
|GET|`/api/users`|ADMIN|List users|
|POST|`/api/upload`|ADMIN|Upload CSV, returns `jobId` (202)|
|GET|`/api/upload/status/{jobId}`|ADMIN|Poll ingestion progress|
|POST|`/api/validate`|ADMIN, OPERATOR|Verify a WID (+ optional image), logs the event|
|GET|`/api/reports/verifications?startDate=\\\&endDate=\\\&page=\\\&size=`|ADMIN, OPERATOR|Paginated verification report|

All endpoints except `/api/auth/login` require `Authorization: Bearer <token>`.

\---

## 6\. Optional / Good-to-Have Features Implemented

* ✅ Login page + admin-driven user creation
* ✅ Two roles (ADMIN, OPERATOR) assignable at creation
* ✅ Role-based access control (upload \& user management are admin-only; validation and
reports are available to both roles, matching "operator should only be able to verify" plus
reporting visibility for follow-up)
* ⏳ Auto-extracting manufacturing/expiry dates from the captured image (OCR) — not
implemented; would use a vision model or an OCR service (e.g. Tesseract / cloud OCR API) on
the captured photo, matched against system data with a confidence threshold before flagging
a mismatch. Noting this as a designed-for-but-not-built extension for the walkthrough.

\---

## 7\. Project Structure

```
pvs/
├── backend/            Spring Boot API 		
│   └── src/main/java/com/pvs/
│       ├── config/       Security, Redis cache, async executor
│       ├── controller/    REST endpoints
│       ├── dto/          Request/response records
│       ├── entity/       JPA entities
│       ├── exception/    Global error handling
│       ├── repository/   Spring Data JPA repositories
│       ├── security/     JWT filter/util, UserDetailsService
│       └── service/      Business logic (CSV ingestion, validation, reports, auth)
├── frontend/           React + Redux Toolkit SPA
│   └── src/
│       ├── api/           Axios client with JWT interceptor
│       ├── components/    Sidebar, ProtectedRoute
│       ├── pages/         Login, Upload, Validate, Reports, User Management
│       └── store/         Redux auth slice
├── scripts/            generate\\\_sample\\\_csv.py — scale-testing helper
└── docker-compose.yml
```

