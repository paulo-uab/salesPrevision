package com.uab.salesprevision.user.repository;

import com.uab.salesprevision.user.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByName(String name);
}
