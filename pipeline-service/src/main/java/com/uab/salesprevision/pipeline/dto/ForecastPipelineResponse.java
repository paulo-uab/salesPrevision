package com.uab.salesprevision.pipeline.dto;

import com.uab.core.enums.FilterOperator;
import com.uab.core.enums.ForecastFieldRole;
import com.uab.core.enums.LogicalOperator;
import com.uab.core.enums.TransformationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "See POST /v1/api/pipelines for the meaning of each forecast configuration field.")
public class ForecastPipelineResponse {

    private Long id;
    private Long templateId;
    private String templateName;
    private String name;
    private String description;
    private Boolean active;
    private String forecastModel;
    private String controlModel;
    private String frequency;
    private Integer forecastHorizon;
    private Integer seasonPeriod;
    private String arimaOrder;
    private Integer nLags;
    private Boolean includeDateFeatures;
    private Boolean incrementalTraining;
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

        @Schema(description = "DATE (temporal dimension), TARGET (to be forecast), GROUP (segments into series), EXOG (exogenous variable) or NONE.")
        private ForecastFieldRole forecastRole;
        private String aggregation;
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
