package com.uab.salesprevision.batch.batch;

import com.uab.core.dto.pipeline.PipelineDto;
import com.uab.salesprevision.batch.client.PipelineClient;
import com.uab.salesprevision.batch.engine.FilterEngine;
import com.uab.salesprevision.batch.engine.TransformationEngine;
import com.uab.salesprevision.batch.entity.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import com.uab.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Scope("step")
@Slf4j
public class ForecastItemProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final PipelineClient pipelineClient;
    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final FilterEngine filterEngine;
    private final TransformationEngine transformationEngine;

    private PipelineDto pipeline;

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
                .orElseThrow(() -> new ResourceNotFoundException("Schedule config não encontrado: " + scheduleConfigId));
        this.pipeline = pipelineClient.getPipeline(config.getPipelineId());
    }

    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        if (!filterEngine.matches(record, pipeline.getFilters())) {
            return null;
        }
        return transformationEngine.apply(record, pipeline.getFields());
    }
}
