package com.uab.salesprevision.template.controller;


import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.dto.IngestionTemplateResponse;
import com.uab.salesprevision.template.dto.UpdateActiveStatusRequest;
import com.uab.salesprevision.template.dto.UpdateIngestionTemplateRequest;
import com.uab.salesprevision.template.service.IngestionTemplateService;
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
public class IngestionTemplateController {

    private final IngestionTemplateService ingestionTemplateService;

    @PostMapping
    public ResponseEntity<IngestionTemplateResponse> create(@Valid @RequestBody CreateIngestionTemplateRequest request) {
        log.debug("POST /v1/api/templates - name='{}', fileType={}, fields={}",
                request.getName(), request.getFileType(),
                request.getFields() != null ? request.getFields().size() : 0);
        IngestionTemplateResponse response = ingestionTemplateService.create(request);
        log.debug("Template created: id={}, name='{}'", response.getId(), response.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngestionTemplateResponse> update(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateIngestionTemplateRequest request) {
        log.debug("PUT /v1/api/templates/{} - name='{}', fileType={}", id, request.getName(), request.getFileType());
        IngestionTemplateResponse response = ingestionTemplateService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<IngestionTemplateResponse> updateActiveStatus(@PathVariable Long id,
                                                                         @Valid @RequestBody UpdateActiveStatusRequest request) {
        log.debug("PATCH /v1/api/templates/{}/active - active={}", id, request.getActive());
        IngestionTemplateResponse response = ingestionTemplateService.updateActiveStatus(id, request.getActive());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<IngestionTemplateResponse>> findAll(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        log.debug("GET /v1/api/templates - active={}, page={}", active, pageable);
        Page<IngestionTemplateResponse> templates = ingestionTemplateService.findAll(active, pageable);
        log.debug("Returning {} of {} templates", templates.getNumberOfElements(), templates.getTotalElements());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngestionTemplateResponse> findById(@PathVariable Long id) {
        log.debug("GET /v1/api/templates/{} - fetching template", id);
        return ResponseEntity.ok(ingestionTemplateService.findById(id));
    }
}
