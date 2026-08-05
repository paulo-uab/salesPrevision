package com.uab.salesprevision.batch.repository;

import com.uab.salesprevision.batch.model.BatchScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchScheduleConfigRepository extends JpaRepository<BatchScheduleConfig, Long> {
    /** Used by the cron scheduler — deliberately unscoped, it processes every company's active schedules. */
    List<BatchScheduleConfig> findByActiveTrue();
    List<BatchScheduleConfig> findByCompanyId(Long companyId);
    Optional<BatchScheduleConfig> findByIdAndCompanyId(Long id, Long companyId);
}
