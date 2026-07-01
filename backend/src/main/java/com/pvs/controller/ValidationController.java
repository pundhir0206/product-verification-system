package com.pvs.controller;

import com.pvs.dto.ValidationDtos.ValidationResponse;
import com.pvs.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/validate")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    /**
     * Operator scans/enters a WID (barcode scanners act as keyboard input,
     * so a plain text field handles that) and optionally attaches a photo
     * captured from the device camera.
     */
    @PostMapping
    public ResponseEntity<ValidationResponse> validate(
            @RequestParam("wid") String wid,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "unknown";
        return ResponseEntity.ok(validationService.validate(wid, image, username));
    }
}
