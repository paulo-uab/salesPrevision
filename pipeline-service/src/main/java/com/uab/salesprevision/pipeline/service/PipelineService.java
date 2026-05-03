package com.uab.salesprevision.pipeline.service;

import com.uab.core.dto.ingestion.TemplateDto;
import com.uab.core.dto.pipeline.PipelineDto;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.pipeline.client.TemplateClient;
import com.uab.salesprevision.pipeline.dto.CreateForecastPipelineRequest;
import com.uab.salesprevision.pipeline.dto.ForecastPipelineResponse;
import com.uab.salesprevision.pipeline.entity.ForecastPipeline;
import com.uab.salesprevision.pipeline.entity.PipelineField;
import com.uab.salesprevision.pipeline.entity.PipelineFilter;
import com.uab.salesprevision.pipeline.mapper.PipelineMapper;
import com.uab.salesprevision.pipeline.repository.ForecastPipelineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final ForecastPipelineRepository repository;
    private final TemplateClient templateClient;
    private final PipelineMapper mapper;

    @Transactional
    public ForecastPipelineResponse create(CreateForecastPipelineRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new BadRequestException("error.pipeline.name.duplicate", request.getName());
        }

        TemplateDto template = templateClient.getTemplate(request.getTemplateId());

        ForecastPipeline pipeline = mapper.toEntity(request);
        pipeline.setTemplateName(template.getName());

        List<PipelineField> fields = request.getFields().stream()
                .map(mapper::toFieldEntity)
                .toList();
        fields.forEach(f -> f.setPipeline(pipeline));
        pipeline.getFields().addAll(fields);

        List<PipelineFilter> filters = request.getFilters().stream()
                .map(mapper::toFilterEntity)
                .toList();
        filters.forEach(f -> f.setPipeline(pipeline));
        pipeline.getFilters().addAll(filters);

        return mapper.toResponse(repository.save(pipeline));
    }

    @Transactional(readOnly = true)
    public List<ForecastPipelineResponse> findAll(Long templateId) {
        List<ForecastPipeline> pipelines = templateId != null
                ? repository.findByTemplateIdOrderByCreatedAtDesc(templateId)
                : repository.findAll();
        return mapper.toResponseList(pipelines);
    }

    @Transactional(readOnly = true)
    public ForecastPipelineResponse findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PipelineDto getDto(Long id) {
        return mapper.toDto(getEntity(id));
    }

    private ForecastPipeline getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.pipeline.not.found", id));
    }
}
