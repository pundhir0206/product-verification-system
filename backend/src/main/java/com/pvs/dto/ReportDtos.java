package com.pvs.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportDtos {

    public record ReportRowResponse(
            Long id,
            String wid,
            String ean,
            LocalDate manufacturingDate,
            LocalDate expiryDate,
            boolean found,
            String operatorUsername,
            String imagePath,
            LocalDateTime verifiedAt) implements Serializable {}

    public record ReportPageResponse(
            List<ReportRowResponse> rows,
            long totalElements,
            int totalPages,
            int page,
            int size) implements Serializable {}
}
