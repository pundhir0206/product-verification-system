package com.pvs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per floor-verification event performed by an operator.
 */
@Entity
@Table(name = "validation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String wid;

    @Column(length = 64)
    private String ean;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** false if the operator scanned a WID that does not exist in the system */
    @Column(nullable = false)
    @Builder.Default
    private boolean found = true;

    @Column(name = "operator_username", nullable = false, length = 100)
    private String operatorUsername;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "verified_at", nullable = false)
    @Builder.Default
    private LocalDateTime verifiedAt = LocalDateTime.now();
}
