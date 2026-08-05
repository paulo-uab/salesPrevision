package com.uab.salesprevision.pipeline.repository;

import com.uab.salesprevision.pipeline.model.ForecastPipeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForecastPipelineRepository extends JpaRepository<ForecastPipeline, Long> {
    boolean existsByNameAndCompanyId(String name, Long companyId);
    Optional<ForecastPipeline> findByNameAndCompanyId(String name, Long companyId);
    Optional<ForecastPipeline> findByIdAndCompanyId(Long id, Long companyId);
    List<ForecastPipeline> findByCompanyId(Long companyId);
    List<ForecastPipeline> findByCompanyIdAndTemplateIdOrderByCreatedAtDesc(Long companyId, Long templateId);
}
