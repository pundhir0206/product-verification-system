package com.pvs.controller;

import com.pvs.dto.UploadDtos.JobStatusResponse;
import com.pvs.dto.UploadDtos.UploadAcceptedResponse;
import com.pvs.entity.UploadJob;
import com.pvs.repository.UploadJobRepository;
import com.pvs.service.CsvIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CsvIngestionService csvIngestionService;
    private final UploadJobRepository uploadJobRepository;

    @PostMapping
    public ResponseEntity<UploadAcceptedResponse> upload(@RequestParam("file") MultipartFile file,
                                                           Authentication authentication) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Copy to a temp file immediately: the multipart stream is only
        // valid for the lifetime of this request, but processing happens
        // asynchronously so it can handle multi-million-row files without
        // the client holding a connection open.
        Path tempFile = Files.createTempFile("pvs-upload-", ".csv");
        file.transferTo(tempFile);

        String username = authentication != null ? authentication.getName() : "unknown";
        UploadJob job = csvIngestionService.createJob(file.getOriginalFilename(), username);

        csvIngestionService.processAsync(job.getId(), tempFile);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                new UploadAcceptedResponse(job.getId(), job.getStatus().name(),
                        "File accepted. Poll /api/upload/status/" + job.getId() + " for progress."));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> status(@PathVariable UUID jobId) {
        UploadJob job = uploadJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        return ResponseEntity.ok(new JobStatusResponse(
                job.getId(), job.getFileName(), job.getStatus().name(),
                job.getTotalRows(), job.getProcessedRows(), job.getInsertedRows(),
                job.getUpdatedRows(), job.getFailedRows(), job.getErrorMessage()));
    }
}
