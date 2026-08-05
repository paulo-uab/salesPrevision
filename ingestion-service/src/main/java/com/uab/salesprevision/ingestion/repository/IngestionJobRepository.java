package com.uab.salesprevision.ingestion.repository;

import com.uab.salesprevision.ingestion.model.IngestionJob;
import com.uab.core.enums.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {
    Optional<IngestionJob> findByIdAndCompanyId(Long id, Long companyId);
    List<IngestionJob> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<IngestionJob> findByCompanyIdAndTemplateIdOrderByCreatedAtDesc(Long companyId, Long templateId);
    List<IngestionJob> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, IngestionStatus status);
    List<IngestionJob> findByCompanyIdAndTemplateIdAndStatusOrderByCreatedAtDesc(Long companyId, Long templateId, IngestionStatus status);
}
