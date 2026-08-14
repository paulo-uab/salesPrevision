package com.uab.salesprevision.ingestion.controller;


import com.uab.salesprevision.ingestion.dto.CreateIngestionJobResponse;
import com.uab.salesprevision.ingestion.dto.IngestedRecordResponse;
import com.uab.salesprevision.ingestion.dto.IngestionErrorResponse;
import com.uab.salesprevision.ingestion.dto.IngestionJobResponse;
import com.uab.core.enums.IngestionStatus;
import com.uab.salesprevision.ingestion.service.IngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Ingestion", description = "File upload, validation against a template, and access to the resulting records and errors.")
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file and create an ingestion job",
            description = "Validates the file against the given template and stores raw + normalized records. "
                    + "By default the job is processed immediately (autoProcess=true); set it to false to process later via POST /{jobId}/process."
    )
    public ResponseEntity<CreateIngestionJobResponse> createJob(
            @Parameter(description = "File to ingest, in the format declared by the template (fileType).")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID of the template to validate this file against.")
            @RequestParam("templateId") Long templateId,
            @Parameter(description = "Whether to validate and process the file synchronously as part of this call.")
            @RequestParam(value = "autoProcess", defaultValue = "true") boolean autoProcess) {

        log.info("POST /v1/api/ingestion/jobs - file='{}', templateId={}, autoProcess={}",
                file != null ? file.getOriginalFilename() : null, templateId, autoProcess);

        CreateIngestionJobResponse response = ingestionService.createJob(file, templateId, autoProcess);
        log.info("Job created: id={}", response.getJobId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{jobId}/process")
    @Operation(summary = "Process a pending job", description = "Validates and normalizes a job that was created with autoProcess=false.")
    public ResponseEntity<Void> processJob(@PathVariable Long jobId) {
        log.info("POST /v1/api/ingestion/jobs/{}/process", jobId);
        ingestionService.submitForProcessing(jobId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    @Operation(summary = "List ingestion jobs", description = "Filter by templateId and/or status; unfiltered returns all jobs for the caller's company.")
    public ResponseEntity<List<IngestionJobResponse>> findAll(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "status", required = false) IngestionStatus status) {

        log.debug("GET /v1/api/ingestion/jobs - templateId={}, status={}", templateId, status);
        return ResponseEntity.ok(ingestionService.findAll(templateId, status));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get a job by ID")
    public ResponseEntity<IngestionJobResponse> findById(@PathVariable Long jobId) {
        log.debug("GET /v1/api/ingestion/jobs/{}", jobId);
        return ResponseEntity.ok(ingestionService.findById(jobId));
    }

    @GetMapping("/{jobId}/records")
    @Operation(summary = "List a job's ingested records (paginated)", description = "Each record carries both the raw and normalized payload, plus its validation status.")
    public ResponseEntity<Page<IngestedRecordResponse>> findRecords(
            @PathVariable Long jobId,
            Pageable pageable) {
        log.debug("GET /v1/api/ingestion/jobs/{}/records - page={}", jobId, pageable.getPageNumber());
        return ResponseEntity.ok(ingestionService.findRecords(jobId, pageable));
    }

    @GetMapping("/{jobId}/errors")
    @Operation(summary = "List a job's validation errors (paginated)", description = "One entry per failed validation rule, with the field name, error type, and offending raw value.")
    public ResponseEntity<Page<IngestionErrorResponse>> findErrors(
            @PathVariable Long jobId,
            Pageable pageable) {
        log.debug("GET /v1/api/ingestion/jobs/{}/errors - page={}", jobId, pageable.getPageNumber());
        return ResponseEntity.ok(ingestionService.findErrors(jobId, pageable));
    }
}
