package com.uab.salesprevision.pipeline.controller;

import com.uab.core.dto.pipeline.PipelineDto;
import com.uab.salesprevision.pipeline.dto.CreateForecastPipelineRequest;
import com.uab.salesprevision.pipeline.dto.ForecastPipelineResponse;
import com.uab.salesprevision.pipeline.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ForecastPipelineResponse create(@Valid @RequestBody CreateForecastPipelineRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<ForecastPipelineResponse> findAll(
            @RequestParam(required = false) Long templateId) {
        return service.findAll(templateId);
    }

    @GetMapping("/{id}")
    public ForecastPipelineResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/dto")
    public PipelineDto getDto(@PathVariable Long id) {
        return service.getDto(id);
    }
}
