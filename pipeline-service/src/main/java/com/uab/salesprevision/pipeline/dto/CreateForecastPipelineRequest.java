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

    @NotNull(message = "O templateId é obrigatório")
    private Long templateId;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 150)
    private String name;

    @Size(max = 1000)
    private String description;

    private Boolean active = true;

    @Valid
    @Size(min = 1, message = "É necessário pelo menos um campo")
    private List<FieldRequest> fields = new ArrayList<>();

    @Valid
    private List<FilterRequest> filters = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldRequest {

        @NotBlank(message = "O nome do campo de origem é obrigatório")
        private String sourceFieldName;

        @NotBlank(message = "O nome do campo de destino é obrigatório")
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

        @NotBlank(message = "O nome do campo é obrigatório")
        private String fieldName;

        @NotNull(message = "O operador é obrigatório")
        private FilterOperator operator;

        private String value;

        private LogicalOperator logicalOperator = LogicalOperator.AND;

        private Integer orderIndex;
    }
}
