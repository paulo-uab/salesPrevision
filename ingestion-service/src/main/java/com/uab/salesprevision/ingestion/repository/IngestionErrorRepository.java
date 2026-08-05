package com.uab.salesprevision.ingestion.repository;

import com.uab.salesprevision.ingestion.model.IngestionError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionErrorRepository extends JpaRepository<IngestionError, Long> {
    Page<IngestionError> findByIngestionJobIdOrderByCreatedAtAsc(Long ingestionJobId, Pageable pageable);
    void deleteByIngestionJobId(Long ingestionJobId);
}
