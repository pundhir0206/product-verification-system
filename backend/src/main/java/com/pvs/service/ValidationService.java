package com.pvs.service;

import com.pvs.dto.ValidationDtos.ValidationResponse;
import com.pvs.entity.Product;
import com.pvs.entity.ValidationLog;
import com.pvs.repository.ProductRepository;
import com.pvs.repository.ValidationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final ProductRepository productRepository;
    private final ValidationLogRepository validationLogRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ValidationResponse validate(String widRaw, MultipartFile image, String operatorUsername) {
        String wid = widRaw == null ? "" : widRaw.trim();
        Optional<Product> productOpt = productRepository.findById(wid);

        String imagePath = fileStorageService.storeValidationImage(wid, image);
        LocalDateTime now = LocalDateTime.now();

        ValidationLog.ValidationLogBuilder logBuilder = ValidationLog.builder()
                .wid(wid)
                .operatorUsername(operatorUsername)
                .imagePath(imagePath)
                .verifiedAt(now);

        if (productOpt.isPresent()) {
            Product p = productOpt.get();
            logBuilder.found(true).ean(p.getEan())
                    .manufacturingDate(p.getManufacturingDate())
                    .expiryDate(p.getExpiryDate());
            validationLogRepository.save(logBuilder.build());

            return new ValidationResponse(wid, true, p.getEan(), p.getManufacturingDate(),
                    p.getExpiryDate(), "Product found. Compare against physical label.", now);
        } else {
            logBuilder.found(false);
            validationLogRepository.save(logBuilder.build());

            return new ValidationResponse(wid, false, null, null, null,
                    "No product found for this WID.", now);
        }
    }
}
