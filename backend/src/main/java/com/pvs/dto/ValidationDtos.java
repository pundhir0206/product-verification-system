package com.pvs.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ValidationDtos {

    public record ValidationResponse(
            String wid,
            boolean found,
            String ean,
            LocalDate manufacturingDate,
            LocalDate expiryDate,
            String message,
            LocalDateTime verifiedAt) {}
}
