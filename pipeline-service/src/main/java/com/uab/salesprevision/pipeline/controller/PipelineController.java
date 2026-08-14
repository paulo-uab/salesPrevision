package com.uab.salesprevision.pipeline.controller;

import com.uab.salesprevision.pipeline.dto.PipelineDto;
import com.uab.salesprevision.pipeline.dto.CreateForecastPipelineRequest;
import com.uab.salesprevision.pipeline.dto.ForecastPipelineResponse;
import com.uab.salesprevision.pipeline.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("v1/api/pipelines")
@RequiredArgsConstructor
@Tag(name = "Pipelines", description = "Forecast pipeline configuration — which fields to extract, transform and send to the prediction-service, and with which model to forecast.")
public class PipelineController {

    private final PipelineService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a forecast pipeline",
            description = "Requires exactly one field with forecastRole=DATE and at least one with forecastRole=TARGET in fields — otherwise returns 400."
    )
    public ForecastPipelineResponse create(@Valid @RequestBody CreateForecastPipelineRequest request) {
        log.info("POST /v1/api/pipelines - name='{}', templateId={}", request.getName(), request.getTemplateId());
        ForecastPipelineResponse response = service.create(request);
        log.info("Pipeline created: id={}", response.getId());
        return response;
    }

    @GetMapping
    @Operation(summary = "List the company's pipelines", description = "Filters by templateId when provided.")
    public List<ForecastPipelineResponse> findAll(
            @RequestParam(required = false) Long templateId) {
        log.debug("GET /v1/api/pipelines - templateId={}", templateId);
        return service.findAll(templateId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a pipeline by ID")
    public ForecastPipelineResponse findById(@PathVariable Long id) {
        log.debug("GET /v1/api/pipelines/{}", id);
        return service.findById(id);
    }

    @GetMapping("/{id}/dto")
    @Operation(
            summary = "Get the DTO consumed by batch-service",
            description = "Internal endpoint — called by batch-service (not end users) to build the payload sent to the prediction-service on each run."
    )
    public PipelineDto getDto(@PathVariable Long id) {
        log.debug("GET /v1/api/pipelines/{}/dto", id);
        return service.getDto(id);
    }
}
