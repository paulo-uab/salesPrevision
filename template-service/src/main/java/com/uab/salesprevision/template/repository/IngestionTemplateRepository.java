package com.uab.salesprevision.template.repository;

import com.uab.salesprevision.template.model.IngestionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngestionTemplateRepository extends JpaRepository<IngestionTemplate, Long> {
    boolean existsByNameAndCompanyId(String name, Long companyId);
    Optional<IngestionTemplate> findByNameAndCompanyId(String name, Long companyId);
    Optional<IngestionTemplate> findByIdAndCompanyId(Long id, Long companyId);
    Page<IngestionTemplate> findByCompanyId(Long companyId, Pageable pageable);
    Page<IngestionTemplate> findByCompanyIdAndActive(Long companyId, Boolean active, Pageable pageable);
}
