package com.uab.salesprevision.batch.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.core.dto.ingestion.IngestionJobResponse;
import com.uab.salesprevision.batch.client.IngestionClient;
import com.uab.salesprevision.batch.entity.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import com.uab.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@Scope("step")
@Slf4j
public class ForecastItemReader implements ItemReader<Map<String, Object>> {

    private final IngestionClient ingestionClient;
    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final ObjectMapper objectMapper;

    @Value("#{jobParameters['scheduleConfigId']}")
    private Long scheduleConfigId;

    private Queue<Map<String, Object>> buffer;

    public ForecastItemReader(IngestionClient ingestionClient,
                              BatchScheduleConfigRepository scheduleConfigRepository,
                              ObjectMapper objectMapper) {
        this.ingestionClient = ingestionClient;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.objectMapper = objectMapper;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.scheduleConfigId = stepExecution.getJobParameters().getLong("scheduleConfigId");
    }

    @Override
    public Map<String, Object> read() {
        if (buffer == null) {
            buffer = loadRecords();
        }
        return buffer.isEmpty() ? null : buffer.poll();
    }

    @SuppressWarnings("unchecked")
    private Queue<Map<String, Object>> loadRecords() {
        BatchScheduleConfig config = scheduleConfigRepository.findById(scheduleConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule config não encontrado: " + scheduleConfigId));

        LocalDateTime cutoff = LocalDateTime.now().minusDays(config.getLookbackDays());

        List<IngestionJobResponse> jobs = ingestionClient.getCompletedJobs(
                config.getPipelineId());

        Queue<Map<String, Object>> result = new LinkedList<>();

        for (IngestionJobResponse job : jobs) {
            if (job.getCreatedAt() != null && job.getCreatedAt().isBefore(cutoff)) {
                continue;
            }
            loadJobRecords(job.getId(), result);
        }

        log.info("ForecastItemReader carregou {} registos para o schedule {}", result.size(), scheduleConfigId);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void loadJobRecords(Long jobId, Queue<Map<String, Object>> result) {
        int page = 0;
        int size = 200;
        boolean hasMore = true;

        while (hasMore) {
            Map<String, Object> pageData = ingestionClient.getRecordsPage(jobId, page, size);
            if (pageData == null) break;

            List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
            if (content == null || content.isEmpty()) break;

            for (Map<String, Object> record : content) {
                String normalized = (String) record.get("normalizedPayload");
                if (normalized != null) {
                    try {
                        Map<String, Object> payload = objectMapper.readValue(normalized, Map.class);
                        result.add(payload);
                    } catch (Exception e) {
                        log.warn("Erro ao desserializar registo do job {}: {}", jobId, e.getMessage());
                    }
                }
            }

            Boolean last = (Boolean) pageData.get("last");
            hasMore = last == null || !last;
            page++;
        }
    }
}
