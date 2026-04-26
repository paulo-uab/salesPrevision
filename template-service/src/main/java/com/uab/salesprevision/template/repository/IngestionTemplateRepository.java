package com.uab.salesprevision.template.repository;

import com.uab.salesprevision.template.entity.IngestionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngestionTemplateRepository extends JpaRepository<IngestionTemplate, Long> {
    boolean existsByName(String name);
    Optional<IngestionTemplate> findByName(String name);
}
