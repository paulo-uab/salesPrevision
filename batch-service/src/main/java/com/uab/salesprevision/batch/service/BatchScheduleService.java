package com.uab.salesprevision.batch.service;

import com.uab.core.dto.pipeline.PipelineDto;
import com.uab.core.enums.BatchExecutionStatus;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.batch.client.PipelineClient;
import com.uab.salesprevision.batch.dto.BatchExecutionResponse;
import com.uab.salesprevision.batch.dto.BatchScheduleResponse;
import com.uab.salesprevision.batch.dto.CreateBatchScheduleRequest;
import com.uab.salesprevision.batch.entity.BatchExecution;
import com.uab.salesprevision.batch.entity.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchExecutionRepository;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final JobLauncher jobLauncher;
    private final Job forecastJob;

    @Transactional
    public BatchScheduleResponse create(CreateBatchScheduleRequest request) {
        PipelineDto pipeline = pipelineClient.getPipeline(request.getPipelineId());

        BatchScheduleConfig config = BatchScheduleConfig.builder()
                .pipelineId(request.getPipelineId())
                .pipelineName(pipeline.getName())
                .cronExpression(request.getCronExpression())
                .lookbackDays(request.getLookbackDays() != null ? request.getLookbackDays() : 7)
                .predictionApiUrl(request.getPredictionApiUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(request.getCreatedBy())
                .build();

        return toResponse(scheduleConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<BatchScheduleResponse> findAll() {
        return scheduleConfigRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchScheduleResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<BatchExecutionResponse> findExecutions(Long scheduleId, Pageable pageable) {
        ensureExists(scheduleId);
        return executionRepository
                .findByScheduleConfigIdOrderByCreatedAtDesc(scheduleId, pageable)
                .map(this::toExecutionResponse);
    }

    @Transactional
    public BatchExecutionResponse triggerManually(Long scheduleId) {
        BatchScheduleConfig config = getEntity(scheduleId);

        if (executionRepository.existsByScheduleConfigIdAndStatus(scheduleId, BatchExecutionStatus.RUNNING)) {
            throw new BadRequestException("error.batch.already.running");
        }

        return runJob(config);
    }

    public BatchExecutionResponse runJob(BatchScheduleConfig config) {
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

            var jobExecution = jobLauncher.run(forecastJob, params);

            execution.setSpringBatchJobExecutionId(jobExecution.getId());
            execution.setStatus(BatchExecutionStatus.COMPLETED);
            execution.setFinishedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Erro na execução do batch para schedule {}: {}", config.getId(), e.getMessage(), e);
            execution.setStatus(BatchExecutionStatus.FAILED);
            execution.setFinishedAt(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
        }

        return toExecutionResponse(executionRepository.save(execution));
    }

    private BatchScheduleConfig getEntity(Long id) {
        return scheduleConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.batch.schedule.not.found", id));
    }

    private void ensureExists(Long id) {
        if (!scheduleConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("error.batch.schedule.not.found", id);
        }
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
