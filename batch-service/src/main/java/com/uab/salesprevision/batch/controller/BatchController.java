package com.uab.salesprevision.batch.controller;

import com.uab.salesprevision.batch.dto.BatchExecutionResponse;
import com.uab.salesprevision.batch.dto.BatchScheduleResponse;
import com.uab.salesprevision.batch.dto.CreateBatchScheduleRequest;
import com.uab.salesprevision.batch.service.BatchScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/batch/schedules")
@RequiredArgsConstructor
public class BatchController {

    private final BatchScheduleService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchScheduleResponse create(@Valid @RequestBody CreateBatchScheduleRequest request) {
        log.info("POST /api/batch/schedules - pipelineId={}, cron='{}'",
                request.getPipelineId(), request.getCronExpression());
        BatchScheduleResponse response = service.create(request);
        log.info("Schedule created: id={}", response.getId());
        return response;
    }

    @GetMapping
    public List<BatchScheduleResponse> findAll() {
        log.debug("GET /api/batch/schedules");
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BatchScheduleResponse findById(@PathVariable Long id) {
        log.debug("GET /api/batch/schedules/{}", id);
        return service.findById(id);
    }

    @GetMapping("/{id}/executions")
    public Page<BatchExecutionResponse> findExecutions(@PathVariable Long id, Pageable pageable) {
        log.debug("GET /api/batch/schedules/{}/executions - page={}", id, pageable.getPageNumber());
        return service.findExecutions(id, pageable);
    }

    @PostMapping("/{id}/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BatchExecutionResponse trigger(@PathVariable Long id) {
        log.info("POST /api/batch/schedules/{}/trigger", id);
        return service.triggerManually(id);
    }
}
