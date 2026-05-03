package com.uab.salesprevision.pipeline.dto;

import com.uab.core.enums.FilterOperator;
import com.uab.core.enums.LogicalOperator;
import com.uab.core.enums.TransformationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ForecastPipelineResponse {

    private Long id;
    private Long templateId;
    private String templateName;
    private String name;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldResponse> fields = new ArrayList<>();
    private List<FilterResponse> filters = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FieldResponse {
        private Long id;
        private String sourceFieldName;
        private String targetFieldName;
        private TransformationType transformationType;
        private String transformationConfig;
        private Integer positionIndex;
        private Boolean active;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FilterResponse {
        private Long id;
        private String fieldName;
        private FilterOperator operator;
        private String value;
        private LogicalOperator logicalOperator;
        private Integer orderIndex;
    }
}
