package com.pvs.repository;

import com.pvs.entity.ValidationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ValidationLogRepository extends JpaRepository<ValidationLog, Long> {

    // Uses idx_validation_logs_verified_at - keeps this fast even as the
    // table grows into the tens of millions of rows.
    @Query("SELECT v FROM ValidationLog v WHERE v.verifiedAt BETWEEN :start AND :end ORDER BY v.verifiedAt DESC")
    Page<ValidationLog> findByVerifiedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT COUNT(v) FROM ValidationLog v WHERE v.verifiedAt BETWEEN :start AND :end")
    long countByVerifiedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
