-- ============================================================
-- Product Verification System - Initial Schema
-- ============================================================

-- ---------- Users & Roles (RBAC) ----------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'OPERATOR')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Products (current authoritative data per physical item) ----------
-- WID is the primary identifier for a single physical item -> strict uniqueness.
CREATE TABLE products (
    wid                 VARCHAR(64)  PRIMARY KEY,
    ean                 VARCHAR(64)  NOT NULL,
    manufacturing_date  DATE         NOT NULL,
    expiry_date         DATE         NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- EAN is looked up frequently (same EAN, many WIDs) -> index it.
CREATE INDEX idx_products_ean ON products (ean);

-- ---------- Upload Jobs (tracks async bulk CSV ingestion) ----------
CREATE TABLE upload_jobs (
    id              UUID PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED')),
    total_rows      BIGINT       NOT NULL DEFAULT 0,
    processed_rows  BIGINT       NOT NULL DEFAULT 0,
    inserted_rows   BIGINT       NOT NULL DEFAULT 0,
    updated_rows    BIGINT       NOT NULL DEFAULT 0,
    failed_rows     BIGINT       NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ---------- Staging table used for high-throughput bulk load ----------
-- Rows are streamed here via JDBC batch/COPY, then merged into `products`
-- with a single set-based UPSERT. Truncated after each job merge.
CREATE TABLE staging_products (
    job_id              UUID         NOT NULL,
    wid                 VARCHAR(64)  NOT NULL,
    ean                 VARCHAR(64),
    manufacturing_date  VARCHAR(32),  -- kept as text; parsed/validated during merge
    expiry_date         VARCHAR(32),
    seq_no          BIGINT
);

CREATE INDEX idx_staging_job_id ON staging_products (job_id);

-- ---------- Validation Logs (every floor verification event) ----------
CREATE TABLE validation_logs (
    id                  BIGSERIAL PRIMARY KEY,
    wid                 VARCHAR(64)  NOT NULL,
    ean                 VARCHAR(64),
    manufacturing_date  DATE,
    expiry_date         DATE,
    found               BOOLEAN      NOT NULL DEFAULT TRUE,
    operator_username   VARCHAR(100) NOT NULL,
    image_path          VARCHAR(500),
    verified_at         TIMESTAMP    NOT NULL DEFAULT now()
);

-- Report queries filter by date range -> this index is the most important
-- one for the "scalability of reporting" requirement.
CREATE INDEX idx_validation_logs_verified_at ON validation_logs (verified_at);
CREATE INDEX idx_validation_logs_wid ON validation_logs (wid);
