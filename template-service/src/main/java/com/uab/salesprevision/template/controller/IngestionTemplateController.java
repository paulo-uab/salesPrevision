package com.uab.salesprevision.template.controller;


import com.uab.core.dto.template.CreateIngestionTemplateRequest;
import com.uab.core.dto.template.IngestionTemplateResponse;
import com.uab.salesprevision.template.service.IngestionTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/api/templates")
@RequiredArgsConstructor
public class IngestionTemplateController {

    private final IngestionTemplateService ingestionTemplateService;

    @PostMapping
    public ResponseEntity<IngestionTemplateResponse> create(@Valid @RequestBody CreateIngestionTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestionTemplateService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<IngestionTemplateResponse>> findAll() {
        return ResponseEntity.ok(ingestionTemplateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngestionTemplateResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ingestionTemplateService.findById(id));
    }
}
