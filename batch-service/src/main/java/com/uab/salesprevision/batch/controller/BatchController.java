package com.uab.salesprevision.batch.controller;

import com.uab.salesprevision.batch.dto.BatchExecutionResponse;
import com.uab.salesprevision.batch.dto.BatchScheduleResponse;
import com.uab.salesprevision.batch.dto.CreateBatchScheduleRequest;
import com.uab.salesprevision.batch.service.BatchScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Batch schedules", description = "Recurring Spring Batch runs that read forecast-ready data, apply a pipeline, and send the result to a prediction API.")
public class BatchController {

    private final BatchScheduleService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a schedule", description = "Ties a pipeline to a cron expression, a lookback window, and a prediction API URL.")
    public BatchScheduleResponse create(@Valid @RequestBody CreateBatchScheduleRequest request) {
        log.info("POST /api/batch/schedules - pipelineId={}, cron='{}'",
                request.getPipelineId(), request.getCronExpression());
        BatchScheduleResponse response = service.create(request);
        log.info("Schedule created: id={}", response.getId());
        return response;
    }

    @GetMapping
    @Operation(summary = "List the company's schedules")
    public List<BatchScheduleResponse> findAll() {
        log.debug("GET /api/batch/schedules");
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a schedule by ID")
    public BatchScheduleResponse findById(@PathVariable Long id) {
        log.debug("GET /api/batch/schedules/{}", id);
        return service.findById(id);
    }

    @GetMapping("/{id}/executions")
    @Operation(summary = "List a schedule's execution history (paginated)", description = "Includes both cron-triggered and manually triggered runs, most recent first.")
    public Page<BatchExecutionResponse> findExecutions(@PathVariable Long id, Pageable pageable) {
        log.debug("GET /api/batch/schedules/{}/executions - page={}", id, pageable.getPageNumber());
        return service.findExecutions(id, pageable);
    }

    @PostMapping("/{id}/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Manually trigger a run", description = "Starts the job immediately, outside of its cron schedule. Fails if a run for this schedule is already in progress.")
    public BatchExecutionResponse trigger(@PathVariable Long id) {
        log.info("POST /api/batch/schedules/{}/trigger", id);
        return service.triggerManually(id);
    }
}
