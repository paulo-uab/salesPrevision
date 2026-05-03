package com.uab.salesprevision.pipeline.repository;

import com.uab.salesprevision.pipeline.entity.ForecastPipeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForecastPipelineRepository extends JpaRepository<ForecastPipeline, Long> {
    boolean existsByName(String name);
    List<ForecastPipeline> findByTemplateIdOrderByCreatedAtDesc(Long templateId);
    Optional<ForecastPipeline> findByName(String name);
}
