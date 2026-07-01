package com.pvs.repository;

import com.pvs.entity.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadJobRepository extends JpaRepository<UploadJob, UUID> {
}
