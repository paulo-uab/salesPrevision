package com.uab.salesprevision.user.controller;

import com.uab.salesprevision.user.dto.CompanyResponse;
import com.uab.salesprevision.user.dto.CreateCompanyRequest;
import com.uab.salesprevision.user.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Tenant management. A company must exist before any user can be assigned to it.")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @Operation(summary = "Create a company")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request) {
        log.debug("POST /api/companies - name='{}'", request.getName());
        CompanyResponse response = companyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all companies")
    public ResponseEntity<List<CompanyResponse>> findAll() {
        log.debug("GET /api/companies");
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a company by ID")
    public ResponseEntity<CompanyResponse> findById(@PathVariable Long id) {
        log.debug("GET /api/companies/{}", id);
        return ResponseEntity.ok(companyService.findById(id));
    }
}
