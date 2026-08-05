package com.uab.salesprevision.pipeline.controller;

import com.uab.salesprevision.pipeline.dto.PipelineDto;
import com.uab.salesprevision.pipeline.dto.CreateForecastPipelineRequest;
import com.uab.salesprevision.pipeline.dto.ForecastPipelineResponse;
import com.uab.salesprevision.pipeline.service.PipelineService;
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
public class PipelineController {

    private final PipelineService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ForecastPipelineResponse create(@Valid @RequestBody CreateForecastPipelineRequest request) {
        log.info("POST /v1/api/pipelines - name='{}', templateId={}", request.getName(), request.getTemplateId());
        ForecastPipelineResponse response = service.create(request);
        log.info("Pipeline created: id={}", response.getId());
        return response;
    }

    @GetMapping
    public List<ForecastPipelineResponse> findAll(
            @RequestParam(required = false) Long templateId) {
        log.debug("GET /v1/api/pipelines - templateId={}", templateId);
        return service.findAll(templateId);
    }

    @GetMapping("/{id}")
    public ForecastPipelineResponse findById(@PathVariable Long id) {
        log.debug("GET /v1/api/pipelines/{}", id);
        return service.findById(id);
    }

    @GetMapping("/{id}/dto")
    public PipelineDto getDto(@PathVariable Long id) {
        log.debug("GET /v1/api/pipelines/{}/dto", id);
        return service.getDto(id);
    }
}
