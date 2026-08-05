package com.uab.salesprevision.batch.service;

import com.uab.salesprevision.batch.client.dto.PipelineClientDto;
import com.uab.core.enums.BatchExecutionStatus;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.batch.client.PipelineClient;
import com.uab.salesprevision.batch.dto.BatchExecutionResponse;
import com.uab.salesprevision.batch.dto.BatchScheduleResponse;
import com.uab.salesprevision.batch.dto.CreateBatchScheduleRequest;
import com.uab.salesprevision.batch.model.BatchExecution;
import com.uab.salesprevision.batch.model.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchExecutionRepository;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchScheduleService {

    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final BatchExecutionRepository executionRepository;
    private final PipelineClient pipelineClient;
    private final JobOperator jobOperator;
    private final Job forecastJob;

    @Transactional
    public BatchScheduleResponse create(CreateBatchScheduleRequest request) {
        Long companyId = currentCompanyId();
        log.info("Creating schedule: pipelineId={}, cron='{}', companyId={}", request.getPipelineId(), request.getCronExpression(), companyId);
        PipelineClientDto pipeline = pipelineClient.getPipeline(request.getPipelineId(), companyId);

        BatchScheduleConfig config = BatchScheduleConfig.builder()
                .companyId(companyId)
                .pipelineId(request.getPipelineId())
                .pipelineName(pipeline.getName())
                .cronExpression(request.getCronExpression())
                .lookbackDays(request.getLookbackDays() != null ? request.getLookbackDays() : 7)
                .predictionApiUrl(request.getPredictionApiUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(request.getCreatedBy())
                .build();

        BatchScheduleResponse response = toResponse(scheduleConfigRepository.save(config));
        log.info("Schedule created: id={}, pipeline='{}'", response.getId(), pipeline.getName());
        return response;
    }

    @Transactional(readOnly = true)
    public List<BatchScheduleResponse> findAll() {
        Long companyId = currentCompanyId();
        log.debug("Listing schedules: companyId={}", companyId);
        List<BatchScheduleResponse> result = scheduleConfigRepository.findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .toList();
        log.debug("Found {} schedules", result.size());
        return result;
    }

    @Transactional(readOnly = true)
    public BatchScheduleResponse findById(Long id) {
        log.debug("Fetching schedule id={}", id);
        return toResponse(getEntity(id, currentCompanyId()));
    }

    @Transactional(readOnly = true)
    public Page<BatchExecutionResponse> findExecutions(Long scheduleId, Pageable pageable) {
        ensureExists(scheduleId, currentCompanyId());
        log.debug("Fetching executions: scheduleId={}, page={}", scheduleId, pageable.getPageNumber());
        return executionRepository
                .findByScheduleConfigIdOrderByCreatedAtDesc(scheduleId, pageable)
                .map(this::toExecutionResponse);
    }

    @Transactional
    public BatchExecutionResponse triggerManually(Long scheduleId) {
        log.info("Manual trigger: scheduleId={}", scheduleId);
        BatchScheduleConfig config = getEntity(scheduleId, currentCompanyId());

        if (executionRepository.existsByScheduleConfigIdAndStatus(scheduleId, BatchExecutionStatus.RUNNING)) {
            log.warn("Schedule id={} already has a running execution", scheduleId);
            throw new BadRequestException("error.batch.already.running");
        }

        return runJob(config);
    }

    public BatchExecutionResponse runJob(BatchScheduleConfig config) {
        log.info("Starting job: scheduleId={}, pipeline='{}'", config.getId(), config.getPipelineName());
        BatchExecution execution = BatchExecution.builder()
                .scheduleConfig(config)
                .status(BatchExecutionStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .recordsSent(0L)
                .recordsFailed(0L)
                .build();
        execution = executionRepository.save(execution);

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("scheduleConfigId", config.getId())
                    .addLong("batchExecutionId", execution.getId())
                    .addLocalDateTime("startedAt", LocalDateTime.now())
                    .toJobParameters();

            var jobExecution = jobOperator.start(forecastJob, params);

            execution.setSpringBatchJobExecutionId(jobExecution.getId());
            execution.setStatus(BatchExecutionStatus.COMPLETED);
            execution.setFinishedAt(LocalDateTime.now());
            log.info("Job completed: scheduleId={}, executionId={}, springJobId={}",
                    config.getId(), execution.getId(), jobExecution.getId());
        } catch (Exception e) {
            log.error("Job failed: scheduleId={}, reason={}", config.getId(), e.getMessage(), e);
            execution.setStatus(BatchExecutionStatus.FAILED);
            execution.setFinishedAt(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
        }

        return toExecutionResponse(executionRepository.save(execution));
    }

    private BatchScheduleConfig getEntity(Long id, Long companyId) {
        return scheduleConfigRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> {
                    log.warn("Schedule not found: id={}, companyId={}", id, companyId);
                    return new ResourceNotFoundException("error.batch.schedule.not.found", id);
                });
    }

    private void ensureExists(Long id, Long companyId) {
        if (scheduleConfigRepository.findByIdAndCompanyId(id, companyId).isEmpty()) {
            log.warn("Schedule not found: id={}, companyId={}", id, companyId);
            throw new ResourceNotFoundException("error.batch.schedule.not.found", id);
        }
    }

    private Long currentCompanyId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Object companyId = jwt.getClaim("companyId");
        if (!(companyId instanceof Number number)) {
            throw new BadRequestException("error.batch.company.missing");
        }
        return number.longValue();
    }

    private BatchScheduleResponse toResponse(BatchScheduleConfig config) {
        BatchScheduleResponse r = new BatchScheduleResponse();
        r.setId(config.getId());
        r.setPipelineId(config.getPipelineId());
        r.setPipelineName(config.getPipelineName());
        r.setCronExpression(config.getCronExpression());
        r.setLookbackDays(config.getLookbackDays());
        r.setPredictionApiUrl(config.getPredictionApiUrl());
        r.setActive(config.getActive());
        r.setCreatedBy(config.getCreatedBy());
        r.setCreatedAt(config.getCreatedAt());
        r.setUpdatedAt(config.getUpdatedAt());
        return r;
    }

    private BatchExecutionResponse toExecutionResponse(BatchExecution exec) {
        BatchExecutionResponse r = new BatchExecutionResponse();
        r.setId(exec.getId());
        r.setScheduleConfigId(exec.getScheduleConfig().getId());
        r.setSpringBatchJobExecutionId(exec.getSpringBatchJobExecutionId());
        r.setStatus(exec.getStatus());
        r.setStartedAt(exec.getStartedAt());
        r.setFinishedAt(exec.getFinishedAt());
        r.setRecordsSent(exec.getRecordsSent());
        r.setRecordsFailed(exec.getRecordsFailed());
        r.setErrorMessage(exec.getErrorMessage());
        r.setCreatedAt(exec.getCreatedAt());
        return r;
    }
}
