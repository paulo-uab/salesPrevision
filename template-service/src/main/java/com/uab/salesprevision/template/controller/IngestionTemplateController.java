package com.uab.salesprevision.template.controller;


import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.dto.IngestionTemplateResponse;
import com.uab.salesprevision.template.dto.UpdateActiveStatusRequest;
import com.uab.salesprevision.template.dto.UpdateIngestionTemplateRequest;
import com.uab.salesprevision.template.service.IngestionTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("v1/api/templates")
@RequiredArgsConstructor
@Tag(name = "Templates", description = "Defines the expected shape of files that can be ingested — columns, types, and validation rules.")
public class IngestionTemplateController {

    private final IngestionTemplateService ingestionTemplateService;

    @PostMapping
    @Operation(summary = "Create a template", description = "Defines a new file shape (format, fields, validation rules) that ingestion-service can validate uploads against.")
    public ResponseEntity<IngestionTemplateResponse> create(@Valid @RequestBody CreateIngestionTemplateRequest request) {
        log.debug("POST /v1/api/templates - name='{}', fileType={}, fields={}",
                request.getName(), request.getFileType(),
                request.getFields() != null ? request.getFields().size() : 0);
        IngestionTemplateResponse response = ingestionTemplateService.create(request);
        log.debug("Template created: id={}, name='{}'", response.getId(), response.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a template's definition", description = "Overwrites name, file format settings, and the entire field list. Bumps the template's version.")
    public ResponseEntity<IngestionTemplateResponse> update(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateIngestionTemplateRequest request) {
        log.debug("PUT /v1/api/templates/{} - name='{}', fileType={}", id, request.getName(), request.getFileType());
        IngestionTemplateResponse response = ingestionTemplateService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a template", description = "Inactive templates cannot be used to create new ingestion jobs, but existing jobs and records are unaffected.")
    public ResponseEntity<IngestionTemplateResponse> updateActiveStatus(@PathVariable Long id,
                                                                         @Valid @RequestBody UpdateActiveStatusRequest request) {
        log.debug("PATCH /v1/api/templates/{}/active - active={}", id, request.getActive());
        IngestionTemplateResponse response = ingestionTemplateService.updateActiveStatus(id, request.getActive());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List templates", description = "Paginated list of templates belonging to the caller's company. Filter by active status with the 'active' query parameter.")
    public ResponseEntity<Page<IngestionTemplateResponse>> findAll(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("GET /v1/api/templates - active={}, page={}", active, pageable);
        Page<IngestionTemplateResponse> templates = ingestionTemplateService.findAll(active, pageable);
        log.debug("Returning {} of {} templates", templates.getNumberOfElements(), templates.getTotalElements());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a template by ID")
    public ResponseEntity<IngestionTemplateResponse> findById(@PathVariable Long id) {
        log.debug("GET /v1/api/templates/{} - fetching template", id);
        return ResponseEntity.ok(ingestionTemplateService.findById(id));
    }
}
