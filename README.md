<div align="center">

# 📦 Product Verification System (PVS)

**A production-ready, full-stack warehouse product verification platform**

Bulk CSV ingestion • JWT authentication • Streaming processing • Audit-grade reporting

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](#)
[![React](https://img.shields.io/badge/React-Vite-61DAFB?logo=react&logoColor=white)](#)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](#)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-Educational%2FPortfolio-lightgrey)](#license)

</div>

---

## 📖 Overview

**PVS** solves a common warehouse problem: importing millions of product records from supplier CSVs, then verifying each item on the floor using a unique **Warehouse ID (WID)** — with a full, immutable audit trail behind every check.

Rather than a simple CRUD demo, this project showcases **production-grade backend engineering**:

- ⚡ Asynchronous, streaming CSV ingestion (constant memory, even at millions of rows)
- 🧱 Hibernate batch processing for high-throughput writes
- 🔐 Stateless JWT authentication with Spring Security
- 🏗️ Clean, layered architecture (Controller → Service → Repository → Entity)
- 🐳 One-command Docker Compose deployment
- 📊 Paginated, audit-based reporting

The application flow, end to end:

```
Login → Upload Product CSV → Background Processing → Monitor Progress
      → Verify Products (WID) → Capture Image → View Verification Reports
```

---

## 📑 Table of Contents

- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Run with Docker Compose](#run-with-docker-compose-recommended)
  - [Run Without Docker](#run-without-docker)
- [Application Workflow](#-application-workflow)
- [System Architecture](#-system-architecture)
- [Core Modules](#-core-modules)
- [CSV Ingestion Pipeline](#-csv-ingestion-pipeline)
- [Key Design Decisions](#-key-design-decisions)
- [API Reference](#-api-reference)
- [Project Structure](#-project-structure)
- [Production Readiness](#-production-readiness)
- [Performance & Scalability](#-performance--scalability)
- [Future Enhancements](#-future-enhancements)
- [Contributing](#-contributing)
- [Author](#-author)

---

## 🛠 Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA (Hibernate), JWT, Maven, OpenCSV, Lombok, HikariCP, Spring Boot Actuator |
| **Frontend** | React, Vite, Material UI, Axios |
| **Database** | MySQL 8 (accessed exclusively via Spring Data JPA — no native SQL, no `JdbcTemplate`, no vendor-specific logic) |
| **Infrastructure** | Docker, Docker Compose |

> The backend is intentionally **database-independent** — no business logic depends on vendor-specific SQL, so swapping the RDBMS is largely a configuration change.

---

## 🚀 Getting Started

### Prerequisites

| Category | Requirement |
|---|---|
| Backend | Java 17+, Maven 3.9+ |
| Frontend | Node.js 20+, npm |
| Infrastructure | Docker, Docker Compose |

```bash
git clone https://github.com/pundhir0206/product-verification-system.git
cd product-verification-system
```

### Run with Docker Compose (Recommended)

```bash
docker compose up --build
```

This spins up MySQL 8, the Spring Boot backend, and the React frontend together.

| Service | URL |
|---|---|
| 🌐 Frontend | http://localhost:3000 |
| 🔌 Backend API | http://localhost:8080 |
| ❤️ Health Check | http://localhost:8080/actuator/health |

**Default administrator** (auto-created on first startup if none exists):

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ADMIN` |

> ⚠️ Change the default credentials immediately in any production deployment.

To stop:

```bash
docker compose down          # stop containers
docker compose down -v       # also wipe DB volume — deletes all data
```

### Run Without Docker

**1. Create the database**

```sql
CREATE DATABASE pvs_db;
```

**2. Configure the connection** in `backend/src/main/resources/application.yml` or via environment variables:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/pvs_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=password
```

**3. Start the backend**

```bash
cd backend
mvn clean install
mvn spring-boot:run
# → http://localhost:8080
```

**4. Start the frontend**

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

Log in with `admin` / `admin123`.

---

## 🔄 Application Workflow

```
   Login
     │
     ▼
Upload Product CSV
     │
     ▼
Background CSV Processing
     │
     ▼
Monitor Upload Progress
     │
     ▼
Verify Products
     │
     ▼
Upload Product Images
     │
     ▼
View Verification Reports
```

1. **Login** — the backend returns a JWT, which the frontend attaches to every subsequent request.
2. **Bulk Upload** — choose a CSV; the backend immediately creates an `UploadJob` and processes it asynchronously.
3. **Monitor Progress** — track total / processed / inserted / updated / failed row counts in real time.
4. **Verify Products** — scan or enter a WID to look up and validate a product, optionally attaching a photo.
5. **Reports** — browse paginated verification history filtered by date range, WID, or status.

---

## 🏗 System Architecture

```
                    Client (React + Material UI)
                              │
                              ▼
                  REST Controllers (API Layer)
                              │
                              ▼
                        Service Layer
          (Business Logic & Transaction Management)
                              │
                              ▼
               Spring Data JPA Repositories
                              │
                              ▼
                       Hibernate ORM
                              │
                              ▼
                          MySQL 8
```

| Layer | Responsibility |
|---|---|
| **Controller** | Exposes REST APIs, validates incoming requests |
| **Service** | Business logic and transaction management |
| **Repository** | Database access via Spring Data JPA |
| **Entity** | Maps to database tables |
| **Security** | Authentication and authorization |
| **Configuration** | Security, JWT, async processing, app config |

Controllers stay thin, services own the logic, and repositories are persistence-only — no business rules leak into the data layer.

---

## 🧩 Core Modules

### 🔐 Authentication
`AuthController → AuthService → UserRepository → JwtUtil`

- BCrypt password hashing
- JWT generation & validation
- Fully stateless (no HTTP sessions)

### 👥 User Management
*(ADMIN only)*
- Create / view users, assign roles (`ADMIN`, `OPERATOR`)
- Passwords are never stored in plain text

### 📤 Bulk CSV Upload
- Streaming, row-by-row parsing via OpenCSV
- Automatic insert-vs-update detection per WID
- Bad rows are skipped, not fatal — the job keeps going

### ✅ Product Verification
- Lookup by Warehouse ID (WID)
- Optional image capture during verification
- Every check creates an immutable `ValidationLog`

### 📊 Reporting
- Built from `ValidationLog`, not live product state — history stays accurate even if product data changes later
- Date-range filters, pagination

### 🖼 Image Management
- Images stored separately from product metadata
- Designed for a drop-in migration to S3 / Azure Blob / MinIO later, with no business-logic changes

---

## 📥 CSV Ingestion Pipeline

```text
User Uploads CSV
        │
        ▼
UploadController receives file
        │
        ▼
Store file temporarily
        │
        ▼
Create UploadJob record  ──►  Return Job ID immediately
        │
        ▼
Background async processing begins
        │
        ▼
Read row (OpenCSV) → Validate → Map to Product entity
        │
        ▼
Existing WID? ── Yes → Update   │   No → Insert
        │
        ▼
Accumulate into batch (default: 500)
        │
        ▼
saveAll(batch)  →  entityManager.flush() & clear()
        │
        ▼
Update UploadJob progress counters
        │
        ▼
Repeat until EOF → delete temp file → mark COMPLETED / FAILED
```

**Why streaming, not bulk-load?** Reading an entire CSV into a `List<Product>` before writing scales memory linearly with file size and risks `OutOfMemoryError` on large files. Processing one row at a time keeps memory **nearly constant** regardless of dataset size.

**Row-level vs. job-level failures:**

| Type | Examples | Behavior |
|---|---|---|
| Row-level | Missing WID, invalid date, malformed row | Skip row, increment failure counter, keep going |
| Job-level | DB unavailable, file unreadable, unexpected exception | Mark job `FAILED`, persist error, stop, clean up temp file |

**Hibernate batching**, tuned via:

```properties
hibernate.jdbc.batch_size=500
hibernate.order_inserts=true
hibernate.order_updates=true
hibernate.batch_versioned_data=true
```

reduces per-record round trips into efficient bulk `saveAll()` calls, while periodic `flush()`/`clear()` prevents the persistence context from ballooning during long-running imports.

---

## 💡 Key Design Decisions

| Decision | Why |
|---|---|
| **Spring Data JPA over JdbcTemplate** | Database independence, less boilerplate, cleaner repositories |
| **Hibernate batching** | Fewer round trips, lower transaction overhead, better throughput on large imports |
| **Streaming CSV parsing** | Constant memory use regardless of file size — no risk of OOM on huge files |
| **Async upload processing** | Keeps the API responsive; avoids blocking a thread for minutes per upload |
| **UploadJob tracking** | Gives visibility into long-running background work without holding an open connection |
| **Immutable ValidationLog** | Preserves historical accuracy for audits even as product data changes over time |

---

## 📡 API Reference

Base URL: `http://localhost:8080/api` · All endpoints require `Authorization: Bearer <jwt-token>` unless noted otherwise.

### Auth

**`POST /auth/login`**
```json
// Request
{ "username": "admin", "password": "admin123" }

// 200 OK
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```
`401` on invalid credentials.

### Users *(ADMIN only)*

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/users` | Create a user (`username`, `password`, `role`) |
| `GET` | `/users` | List all users |

### CSV Upload

**`POST /upload`** — `multipart/form-data`, field `file`

```json
// 202 Accepted
{ "jobId": 17, "status": "PROCESSING" }
```

**`GET /upload/{jobId}`**

```json
{
  "jobId": 17,
  "status": "PROCESSING",
  "totalRows": 500000,
  "processedRows": 185000,
  "insertedRows": 160000,
  "updatedRows": 22000,
  "failedRows": 3000
}
```
Status values: `PENDING` · `PROCESSING` · `COMPLETED` · `FAILED`

### Product Verification

**`POST /validate`**
```json
// Request
{ "wid": "WID-1000456" }

// 200 OK — found
{
  "status": "SUCCESS",
  "wid": "WID-1000456",
  "ean": "8901234567890",
  "manufacturingDate": "2026-01-15",
  "expiryDate": "2027-01-15"
}

// 200 OK — not found
{ "status": "FAILED", "message": "Product not found" }
```

Image capture is supported as `multipart/form-data` with `wid` + `image` fields.

### Reports

**`GET /reports?page=0&size=20`**
```json
{
  "content": [
    { "wid": "WID10001", "verifiedBy": "operator1", "verifiedAt": "2026-07-01T10:15:30" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1520,
  "totalPages": 76
}
```

### Status Codes

| Code | Meaning |
|---|---|
| 200 | Success |
| 201 | Resource created |
| 202 | Accepted for async processing |
| 400 | Invalid request |
| 401 | Authentication failed |
| 403 | Access denied |
| 404 | Not found |
| 409 | Duplicate resource |
| 500 | Internal server error |

---

## 📁 Project Structure

```text
pvs/
├── backend/
│   ├── src/main/java/com/pvs
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   ├── exception/
│   │   ├── util/
│   │   └── PvsApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── static/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── utils/
│   │   └── App.jsx
│   ├── Dockerfile
│   └── package.json
│
├── docker-compose.yml
├── README.md
└── .gitignore
```

---

## ✅ Production Readiness

- **Stateless JWT auth** — no session replication, trivially horizontally scalable
- **Database independence** — JPA-only persistence, no vendor lock-in
- **Layered architecture** — clear separation of controller / service / repository
- **Async processing** — background CSV ingestion with progress tracking
- **Declarative transactions** — `@Transactional` around creation, updates, batch writes, and logging
- **Hibernate batching** — periodic `flush()`/`clear()` to bound persistence-context memory
- **HikariCP connection pooling** — configurable pool size, idle connections, timeouts
- **Dockerized** — MySQL, backend, and frontend all containerized with persistent volumes
- **Actuator health checks** — `/actuator/health` for orchestrators and load balancers

---

## ⚙️ Performance & Scalability

| Optimization | Benefit |
|---|---|
| Streaming CSV parsing | Constant memory regardless of file size |
| Batched DB writes | Fewer SQL round trips, higher throughput |
| Persistence-context cleanup | Prevents `EntityManager` from retaining millions of entities |
| Pagination on reports | Predictable, fast responses at any data volume |
| Indexing (`wid`, `ean`, `verifiedAt`, `status`, `createdAt`) | Keeps lookups fast as tables grow |

**Scaling out:** the backend is stateless, so multiple instances can run behind a load balancer, each independently handling auth, verification, and reporting. Image storage can move to S3 / Azure Blob / MinIO without touching business logic, and CSV ingestion could be distributed further via Kafka or RabbitMQ if throughput demands grow.

---

## 🔮 Future Enhancements

- Redis caching for frequently accessed reports
- Advanced search using JPA Specifications
- OCR-based extraction of manufacturing/expiry dates from images
- Barcode/QR code generation
- Email notifications on upload completion
- Analytics dashboard for audit data
- Cloud object storage for images
- CI/CD pipeline (GitHub Actions)
- Kubernetes deployment manifests
- Prometheus + Grafana monitoring
- Distributed CSV processing via Kafka
- Fine-grained, role-based permissions
- OpenAPI/Swagger documentation
- Unit, integration, and end-to-end test suites
- Multi-factor authentication
- Soft delete + Spring Data JPA auditing

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Open a Pull Request

Please keep contributions aligned with the project's layered architecture and existing coding conventions.

---

## 👤 Author

**Utkarsh Pundhir**
Backend Software Engineer

`Java` · `Spring Boot` · `Spring Security` · `Spring Data JPA` · `Hibernate` · `MySQL` · `React` · `Docker` · `JWT` · `REST APIs`

---

## License

Provided for educational and portfolio purposes — demonstrating production-oriented backend engineering with the Spring Boot ecosystem: scalable data ingestion, secure authentication, asynchronous processing, clean architecture, and containerized deployment.
