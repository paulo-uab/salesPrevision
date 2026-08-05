package com.uab.salesprevision.pipeline.mapper;

import com.uab.salesprevision.pipeline.dto.PipelineDto;
import com.uab.salesprevision.pipeline.dto.CreateForecastPipelineRequest;
import com.uab.salesprevision.pipeline.dto.ForecastPipelineResponse;
import com.uab.salesprevision.pipeline.model.ForecastPipeline;
import com.uab.salesprevision.pipeline.model.PipelineField;
import com.uab.salesprevision.pipeline.model.PipelineFilter;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PipelineMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "templateName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "fields", ignore = true)
    @Mapping(target = "filters", ignore = true)
    ForecastPipeline toEntity(CreateForecastPipelineRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    PipelineField toFieldEntity(CreateForecastPipelineRequest.FieldRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pipeline", ignore = true)
    PipelineFilter toFilterEntity(CreateForecastPipelineRequest.FilterRequest request);

    ForecastPipelineResponse toResponse(ForecastPipeline pipeline);

    List<ForecastPipelineResponse> toResponseList(List<ForecastPipeline> pipelines);

    PipelineDto toDto(ForecastPipeline pipeline);

    @AfterMapping
    default void linkFields(@MappingTarget ForecastPipeline pipeline) {
        if (pipeline.getFields() != null) {
            pipeline.getFields().forEach(f -> f.setPipeline(pipeline));
        }
        if (pipeline.getFilters() != null) {
            pipeline.getFilters().forEach(f -> f.setPipeline(pipeline));
        }
    }
}
