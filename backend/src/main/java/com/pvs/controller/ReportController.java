package com.pvs.controller;

import com.pvs.dto.ReportDtos.ReportPageResponse;
import com.pvs.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/verifications")
    public ResponseEntity<ReportPageResponse> getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().build();
        }
        // cap page size so a careless client can't force a huge in-memory response
        int safeSize = Math.min(size, 500);

        return ResponseEntity.ok(reportService.getReport(startDate, endDate, page, safeSize));
    }
}
