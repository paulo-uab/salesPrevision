package com.uab.salesprevision.batch.batch;

import com.uab.salesprevision.batch.client.dto.PipelineClientDto;
import com.uab.salesprevision.batch.client.PipelineClient;
import com.uab.salesprevision.batch.engine.FilterEngine;
import com.uab.salesprevision.batch.engine.TransformationEngine;
import com.uab.salesprevision.batch.model.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import com.uab.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@StepScope
@Slf4j
public class ForecastItemProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final PipelineClient pipelineClient;
    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final FilterEngine filterEngine;
    private final TransformationEngine transformationEngine;

    private PipelineClientDto pipeline;

    public ForecastItemProcessor(PipelineClient pipelineClient,
                                 BatchScheduleConfigRepository scheduleConfigRepository,
                                 FilterEngine filterEngine,
                                 TransformationEngine transformationEngine) {
        this.pipelineClient = pipelineClient;
        this.scheduleConfigRepository = scheduleConfigRepository;
        this.filterEngine = filterEngine;
        this.transformationEngine = transformationEngine;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        Long scheduleConfigId = stepExecution.getJobParameters().getLong("scheduleConfigId");
        BatchScheduleConfig config = scheduleConfigRepository.findById(scheduleConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("error.batch.schedule.not.found", scheduleConfigId));
        this.pipeline = pipelineClient.getPipeline(config.getPipelineId(), config.getCompanyId());
        log.info("ForecastItemProcessor initialised: scheduleId={}, pipeline='{}', filters={}, fields={}",
                scheduleConfigId, pipeline.getName(),
                pipeline.getFilters() != null ? pipeline.getFilters().size() : 0,
                pipeline.getFields() != null ? pipeline.getFields().size() : 0);
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        if (!filterEngine.matches(record, pipeline.getFilters())) {
            log.debug("Record filtered out by pipeline '{}'", pipeline.getName());
            return null;
        }
        return transformationEngine.apply(record, pipeline.getFields());
    }
}
