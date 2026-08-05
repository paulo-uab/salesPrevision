package com.uab.salesprevision.pipeline.dto;

import com.uab.core.enums.FilterOperator;
import com.uab.core.enums.LogicalOperator;
import com.uab.core.enums.TransformationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateForecastPipelineRequest {

    @NotNull(message = "{validation.pipeline.template.id.required}")
    private Long templateId;

    @NotBlank(message = "{validation.pipeline.name.required}")
    @Size(max = 150, message = "{validation.pipeline.name.size}")
    private String name;

    @Size(max = 1000, message = "{validation.pipeline.description.size}")
    private String description;

    private Boolean active = true;

    @Valid
    @NotNull(message = "{validation.pipeline.fields.required}")
    @Size(min = 1, message = "{validation.pipeline.fields.required}")
    private List<FieldRequest> fields = new ArrayList<>();

    @Valid
    private List<FilterRequest> filters = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldRequest {

        @NotBlank(message = "{validation.pipeline.field.source.required}")
        private String sourceFieldName;

        @NotBlank(message = "{validation.pipeline.field.target.required}")
        private String targetFieldName;

        private TransformationType transformationType = TransformationType.NONE;

        private String transformationConfig;

        private Integer positionIndex;

        private Boolean active = true;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FilterRequest {

        @NotBlank(message = "{validation.pipeline.filter.field.required}")
        private String fieldName;

        @NotNull(message = "{validation.pipeline.filter.operator.required}")
        private FilterOperator operator;

        private String value;

        private LogicalOperator logicalOperator = LogicalOperator.AND;

        private Integer orderIndex;
    }
}
