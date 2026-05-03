package com.uab.salesprevision.batch.repository;

import com.uab.core.enums.BatchExecutionStatus;
import com.uab.salesprevision.batch.entity.BatchExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExecutionRepository extends JpaRepository<BatchExecution, Long> {
    Page<BatchExecution> findByScheduleConfigIdOrderByCreatedAtDesc(Long scheduleConfigId, Pageable pageable);
    boolean existsByScheduleConfigIdAndStatus(Long scheduleConfigId, BatchExecutionStatus status);
}
