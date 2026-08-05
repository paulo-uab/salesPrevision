package com.uab.salesprevision.ingestion.controller;


import com.uab.salesprevision.ingestion.dto.CreateIngestionJobResponse;
import com.uab.salesprevision.ingestion.dto.IngestedRecordResponse;
import com.uab.salesprevision.ingestion.dto.IngestionErrorResponse;
import com.uab.salesprevision.ingestion.dto.IngestionJobResponse;
import com.uab.core.enums.IngestionStatus;
import com.uab.salesprevision.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/api/ingestion/jobs")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateIngestionJobResponse> createJob(
            @RequestParam("file") MultipartFile file,
            @RequestParam("templateId") Long templateId,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "autoProcess", defaultValue = "true") boolean autoProcess) {

        log.info("POST /v1/api/ingestion/jobs - file='{}', templateId={}, autoProcess={}",
                file != null ? file.getOriginalFilename() : null, templateId, autoProcess);

        CreateIngestionJobResponse response = ingestionService.createJob(file, templateId, createdBy, autoProcess);
        log.info("Job created: id={}", response.getJobId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{jobId}/process")
    public ResponseEntity<Void> processJob(@PathVariable Long jobId) {
        log.info("POST /v1/api/ingestion/jobs/{}/process", jobId);
        ingestionService.submitForProcessing(jobId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public ResponseEntity<List<IngestionJobResponse>> findAll(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "status", required = false) IngestionStatus status) {

        log.debug("GET /v1/api/ingestion/jobs - templateId={}, status={}", templateId, status);
        return ResponseEntity.ok(ingestionService.findAll(templateId, status));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<IngestionJobResponse> findById(@PathVariable Long jobId) {
        log.debug("GET /v1/api/ingestion/jobs/{}", jobId);
        return ResponseEntity.ok(ingestionService.findById(jobId));
    }

    @GetMapping("/{jobId}/records")
    public ResponseEntity<Page<IngestedRecordResponse>> findRecords(
            @PathVariable Long jobId,
            Pageable pageable) {
        log.debug("GET /v1/api/ingestion/jobs/{}/records - page={}", jobId, pageable.getPageNumber());
        return ResponseEntity.ok(ingestionService.findRecords(jobId, pageable));
    }

    @GetMapping("/{jobId}/errors")
    public ResponseEntity<Page<IngestionErrorResponse>> findErrors(
            @PathVariable Long jobId,
            Pageable pageable) {
        log.debug("GET /v1/api/ingestion/jobs/{}/errors - page={}", jobId, pageable.getPageNumber());
        return ResponseEntity.ok(ingestionService.findErrors(jobId, pageable));
    }
}
