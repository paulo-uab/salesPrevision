package com.uab.salesprevision.batch.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.core.enums.ForecastFieldRole;
import com.uab.core.serviceauth.ServiceTokenProvider;
import com.uab.salesprevision.batch.client.PipelineClient;
import com.uab.salesprevision.batch.client.dto.PipelineClientDto;
import com.uab.salesprevision.batch.model.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchExecutionRepository;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import com.uab.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@StepScope
@Slf4j
public class ForecastItemWriter implements ItemWriter<Map<String, Object>> {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final BatchExecutionRepository executionRepository;
    private final PipelineClient pipelineClient;
    private final ServiceTokenProvider serviceTokenProvider;

    private BatchScheduleConfig scheduleConfig;
    private PipelineClientDto pipeline;
    private Long batchExecutionId;

    public ForecastItemWriter(RestClient.Builder builder,
                              ObjectMapper objectMapper,
                              BatchScheduleConfigRepository scheduleConfigRepository,
                              BatchExecutionRepository executionRepository,
                              PipelineClient pipelineClient,
                              ServiceTokenProvider serviceTokenProvider) {
        this.restClient = builder.build();
        this.objectMapper = objectMapper;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.executionRepository = executionRepository;
        this.pipelineClient = pipelineClient;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        Long scheduleConfigId = stepExecution.getJobParameters().getLong("scheduleConfigId");
        this.scheduleConfig = scheduleConfigRepository.findById(scheduleConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("error.batch.schedule.not.found", scheduleConfigId));
        this.pipeline = pipelineClient.getPipeline(scheduleConfig.getPipelineId(), scheduleConfig.getCompanyId());
        this.batchExecutionId = stepExecution.getJobParameters().getLong("batchExecutionId");
        log.debug("ForecastItemWriter initialised: scheduleId={}, executionId={}, url='{}'",
                scheduleConfigId, batchExecutionId, scheduleConfig.getPredictionApiUrl());
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
        payload.put("config", buildForecastConfig());

        try {
            restClient.post()
                    .uri(scheduleConfig.getPredictionApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (Boolean.TRUE.equals(scheduleConfig.getInternalPrediction())) {
                            headers.setBearerAuth(serviceTokenProvider.getToken());
                        }
                    })
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            executionRepository.findById(batchExecutionId).ifPresent(exec -> {
                exec.setRecordsSent(exec.getRecordsSent() + records.size());
                executionRepository.save(exec);
            });

            log.info("Sent {} records to '{}'", records.size(), scheduleConfig.getPredictionApiUrl());
        } catch (Exception e) {
            executionRepository.findById(batchExecutionId).ifPresent(exec -> {
                exec.setRecordsFailed(exec.getRecordsFailed() + records.size());
                executionRepository.save(exec);
            });
            log.error("Failed to send {} records to '{}': {}", records.size(),
                    scheduleConfig.getPredictionApiUrl(), e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to send records to prediction API: " + e.getMessage(), e);
        }
    }

    // Deriva date_field/target_fields/group_field a partir dos forecastRole já
    // definidos em cada PipelineField, em vez de exigir uma segunda configuração
    // registada à parte no prediction-service (era a fonte da dessincronização).
    private Map<String, Object> buildForecastConfig() {
        String dateField = null;
        String groupField = null;
        List<Map<String, Object>> targetFields = new ArrayList<>();
        List<Map<String, Object>> exogFields = new ArrayList<>();

        for (PipelineClientDto.FieldDto field : pipeline.getFields()) {
            if (!Boolean.TRUE.equals(field.getActive()) || field.getForecastRole() == null) {
                continue;
            }
            switch (field.getForecastRole()) {
                case DATE -> dateField = field.getTargetFieldName();
                case GROUP -> groupField = field.getTargetFieldName();
                case TARGET -> {
                    Map<String, Object> targetField = new HashMap<>();
                    targetField.put("field_name", field.getTargetFieldName());
                    targetField.put("aggregation", field.getAggregation() != null ? field.getAggregation() : "sum");
                    targetFields.add(targetField);
                }
                case EXOG -> {
                    Map<String, Object> exogField = new HashMap<>();
                    exogField.put("field_name", field.getTargetFieldName());
                    exogField.put("aggregation", field.getAggregation() != null ? field.getAggregation() : "sum");
                    exogFields.add(exogField);
                }
                case NONE -> { }
            }
        }

        if (dateField == null || targetFields.isEmpty()) {
            throw new IllegalStateException(
                    "Pipeline '" + pipeline.getName() + "' has no field marked as DATE or TARGET for forecasting.");
        }

        Map<String, Object> config = new HashMap<>();
        config.put("date_field", dateField);
        config.put("target_fields", targetFields);
        config.put("exog_fields", exogFields);
        config.put("group_field", groupField);
        config.put("forecast_horizon", pipeline.getForecastHorizon());
        config.put("model", pipeline.getForecastModel());
        config.put("control_model", pipeline.getControlModel());
        config.put("frequency", pipeline.getFrequency());
        config.put("season_period", pipeline.getSeasonPeriod());
        config.put("arima_order", parseArimaOrder(pipeline.getArimaOrder()));
        config.put("n_lags", pipeline.getNLags());
        config.put("include_date_features", pipeline.getIncludeDateFeatures());
        config.put("incremental_training", pipeline.getIncrementalTraining());
        return config;
    }

    private List<Integer> parseArimaOrder(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }
}
