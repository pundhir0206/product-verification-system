package com.pvs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks the lifecycle of a bulk CSV ingestion job so the client can poll
 * progress instead of holding open a long-lived HTTP request for a
 * multi-million-row file.
 */
@Entity
@Table(name = "upload_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadJob {

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "total_rows", nullable = false)
    @Builder.Default
    private long totalRows = 0;

    @Column(name = "processed_rows", nullable = false)
    @Builder.Default
    private long processedRows = 0;

    @Column(name = "inserted_rows", nullable = false)
    @Builder.Default
    private long insertedRows = 0;

    @Column(name = "updated_rows", nullable = false)
    @Builder.Default
    private long updatedRows = 0;

    @Column(name = "failed_rows", nullable = false)
    @Builder.Default
    private long failedRows = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
