package com.uab.salesprevision.batch.repository;

import com.uab.salesprevision.batch.entity.BatchScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchScheduleConfigRepository extends JpaRepository<BatchScheduleConfig, Long> {
    List<BatchScheduleConfig> findByActiveTrue();
    List<BatchScheduleConfig> findByPipelineId(Long pipelineId);
}
