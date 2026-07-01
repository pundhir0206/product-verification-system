package com.pvs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.storage.image-dir}")
    private String imageDir;

    /**
     * Stores an uploaded validation image on disk, namespaced by day, and
     * returns a relative path that can be persisted on the ValidationLog row.
     * Swap this implementation for an S3/GCS client in production without
     * touching callers.
     */
    public String storeValidationImage(String wid, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }
        try {
            String datePart = LocalDate.now().toString();
            Path dir = Paths.get(imageDir, datePart);
            Files.createDirectories(dir);

            String ext = extractExtension(image.getOriginalFilename());
            String fileName = wid + "_" + UUID.randomUUID() + ext;
            Path target = dir.resolve(fileName);

            image.transferTo(target);
            return datePart + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store validation image", e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
