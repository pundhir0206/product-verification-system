package com.pvs.service;

import com.pvs.dto.ReportDtos.ReportPageResponse;
import com.pvs.dto.ReportDtos.ReportRowResponse;
import com.pvs.entity.ValidationLog;
import com.pvs.repository.ValidationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ValidationLogRepository validationLogRepository;

    /**
     * Cached per (start,end,page,size) key for 2 minutes (see RedisConfig).
     * QA managers commonly re-run the same date range while triaging, so this
     * avoids re-scanning validation_logs on every click.
     */
    @Cacheable(value = "validationReport", key = "#startDate + '_' + #endDate + '_' + #page + '_' + #size")
    public ReportPageResponse getReport(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        Page<ValidationLog> result = validationLogRepository.findByVerifiedAtBetween(
                start, end, PageRequest.of(page, size));

        var rows = result.getContent().stream()
                .map(v -> new ReportRowResponse(
                        v.getId(), v.getWid(), v.getEan(), v.getManufacturingDate(), v.getExpiryDate(),
                        v.isFound(), v.getOperatorUsername(), v.getImagePath(), v.getVerifiedAt()))
                .toList();

        return new ReportPageResponse(rows, result.getTotalElements(), result.getTotalPages(), page, size);
    }
}
