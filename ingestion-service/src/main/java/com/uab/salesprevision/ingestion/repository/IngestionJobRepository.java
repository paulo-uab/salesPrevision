package com.uab.salesprevision.ingestion.repository;

import com.uab.salesprevision.ingestion.entity.IngestionJob;
import com.uab.core.enums.IngestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {
    List<IngestionJob> findByTemplateIdOrderByCreatedAtDesc(Long templateId);
    List<IngestionJob> findByStatusOrderByCreatedAtDesc(IngestionStatus status);
    List<IngestionJob> findByTemplateIdAndStatusOrderByCreatedAtDesc(Long templateId, IngestionStatus status);
}
