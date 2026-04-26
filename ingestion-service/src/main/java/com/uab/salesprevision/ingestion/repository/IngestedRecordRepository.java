package com.uab.salesprevision.ingestion.repository;

import com.uab.salesprevision.ingestion.entity.IngestedRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestedRecordRepository extends JpaRepository<IngestedRecord, Long> {
    Page<IngestedRecord> findByIngestionJobIdOrderByRecordIndexAsc(Long ingestionJobId, Pageable pageable);
    void deleteByIngestionJobId(Long ingestionJobId);
}
