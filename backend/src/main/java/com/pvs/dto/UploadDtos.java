package com.pvs.dto;

import java.util.UUID;

public class UploadDtos {

    public record UploadAcceptedResponse(
            UUID jobId,
            String status,
            String message) {}

    public record JobStatusResponse(
            UUID jobId,
            String fileName,
            String status,
            long totalRows,
            long processedRows,
            long insertedRows,
            long updatedRows,
            long failedRows,
            String errorMessage) {}
}
