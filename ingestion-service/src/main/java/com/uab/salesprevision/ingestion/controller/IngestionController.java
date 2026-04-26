package com.uab.salesprevision.ingestion.controller;


import com.uab.core.dto.ingestion.CreateIngestionJobResponse;
import com.uab.core.dto.ingestion.IngestedRecordResponse;
import com.uab.core.dto.ingestion.IngestionErrorResponse;
import com.uab.core.dto.ingestion.IngestionJobResponse;
import com.uab.core.enums.IngestionStatus;
import com.uab.salesprevision.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ingestion/jobs")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateIngestionJobResponse> createJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") Long templateId,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "autoProcess", defaultValue = "true") boolean autoProcess) {

        CreateIngestionJobResponse response = ingestionService.createJob(
                file,
                templateId,
                createdBy,
                autoProcess
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{jobId}/process")
    public ResponseEntity<IngestionJobResponse> processJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(ingestionService.processJob(jobId));
    }

    @GetMapping
    public ResponseEntity<List<IngestionJobResponse>> findAll(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "status", required = false) IngestionStatus status) {

        return ResponseEntity.ok(ingestionService.findAll(templateId, status));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<IngestionJobResponse> findById(@PathVariable Long jobId) {
        return ResponseEntity.ok(ingestionService.findById(jobId));
    }

    @GetMapping("/{jobId}/records")
    public ResponseEntity<Page<IngestedRecordResponse>> findRecords(
            @PathVariable Long jobId,
            Pageable pageable) {
        return ResponseEntity.ok(ingestionService.findRecords(jobId, pageable));
    }

    @GetMapping("/{jobId}/errors")
    public ResponseEntity<Page<IngestionErrorResponse>> findErrors(
            @PathVariable Long jobId,
            Pageable pageable) {
        return ResponseEntity.ok(ingestionService.findErrors(jobId, pageable));
    }
}
