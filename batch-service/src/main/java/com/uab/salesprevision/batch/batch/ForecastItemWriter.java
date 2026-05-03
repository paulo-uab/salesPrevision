package com.uab.salesprevision.batch.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.salesprevision.batch.entity.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchExecutionRepository;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import com.uab.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("step")
@Slf4j
public class ForecastItemWriter implements ItemWriter<Map<String, Object>> {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final BatchExecutionRepository executionRepository;

    private BatchScheduleConfig scheduleConfig;
    private Long batchExecutionId;

    public ForecastItemWriter(RestClient.Builder builder,
                              ObjectMapper objectMapper,
                              BatchScheduleConfigRepository scheduleConfigRepository,
                              BatchExecutionRepository executionRepository) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.executionRepository = executionRepository;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        Long scheduleConfigId = stepExecution.getJobParameters().getLong("scheduleConfigId");
        this.scheduleConfig = scheduleConfigRepository.findById(scheduleConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule config não encontrado: " + scheduleConfigId));
        this.batchExecutionId = stepExecution.getJobParameters().getLong("batchExecutionId");
    }

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) {
        List<? extends Map<String, Object>> records = chunk.getItems();
        if (records.isEmpty()) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("pipelineId", scheduleConfig.getPipelineId());
        payload.put("pipelineName", scheduleConfig.getPipelineName());
        payload.put("scheduleConfigId", scheduleConfig.getId());
        payload.put("batchExecutionId", batchExecutionId);
        payload.put("recordCount", records.size());
        payload.put("records", records);

        try {
            restClient.post()
                    .uri(scheduleConfig.getPredictionApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            executionRepository.findById(batchExecutionId).ifPresent(exec -> {
                exec.setRecordsSent(exec.getRecordsSent() + records.size());
                executionRepository.save(exec);
            });

            log.info("Enviados {} registos para {}", records.size(), scheduleConfig.getPredictionApiUrl());
        } catch (Exception e) {
            executionRepository.findById(batchExecutionId).ifPresent(exec -> {
                exec.setRecordsFailed(exec.getRecordsFailed() + records.size());
                executionRepository.save(exec);
            });
            log.error("Erro ao enviar registos para a API de previsão: {}", e.getMessage());
            throw new RuntimeException("Falha ao enviar para a API de previsão: " + e.getMessage(), e);
        }
    }


}
