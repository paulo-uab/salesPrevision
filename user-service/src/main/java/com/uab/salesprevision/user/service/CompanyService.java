package com.uab.salesprevision.user.service;

import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.user.dto.CompanyResponse;
import com.uab.salesprevision.user.dto.CreateCompanyRequest;
import com.uab.salesprevision.user.model.Company;
import com.uab.salesprevision.user.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        String name = request.getName().trim();
        if (companyRepository.existsByName(name)) {
            log.warn("Duplicate company name: '{}'", name);
            throw new BadRequestException("error.company.name.duplicate", name);
        }
        Company saved = companyRepository.save(Company.builder().name(name).build());
        log.info("Company created: id={}, name='{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {
        return companyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Company getEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Company not found: id={}", id);
                    return new ResourceNotFoundException("error.company.not.found", id);
                });
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .build();
    }
}
