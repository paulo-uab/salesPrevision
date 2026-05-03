package com.uab.salesprevision.batch.controller;

import com.uab.salesprevision.batch.dto.BatchExecutionResponse;
import com.uab.salesprevision.batch.dto.BatchScheduleResponse;
import com.uab.salesprevision.batch.dto.CreateBatchScheduleRequest;
import com.uab.salesprevision.batch.service.BatchScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batch/schedules")
@RequiredArgsConstructor
public class BatchController {

    private final BatchScheduleService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchScheduleResponse create(@Valid @RequestBody CreateBatchScheduleRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<BatchScheduleResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BatchScheduleResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/executions")
    public Page<BatchExecutionResponse> findExecutions(@PathVariable Long id, Pageable pageable) {
        return service.findExecutions(id, pageable);
    }

    @PostMapping("/{id}/trigger")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BatchExecutionResponse trigger(@PathVariable Long id) {
        return service.triggerManually(id);
    }
}
