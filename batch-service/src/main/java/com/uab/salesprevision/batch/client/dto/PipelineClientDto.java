package com.uab.salesprevision.batch.client.dto;

import com.uab.core.enums.FilterOperator;
import com.uab.core.enums.ForecastFieldRole;
import com.uab.core.enums.LogicalOperator;
import com.uab.core.enums.TransformationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PipelineClientDto {

    private Long id;
    private Long templateId;
    private String templateName;
    private String name;
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
    private List<FieldDto> fields = new ArrayList<>();
    private List<FilterDto> filters = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDto {
        private Long id;
        private String sourceFieldName;
        private String targetFieldName;
        private TransformationType transformationType;
        private String transformationConfig;
        private Integer positionIndex;
        private Boolean active;
        private ForecastFieldRole forecastRole;
        private String aggregation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterDto {
        private Long id;
        private String fieldName;
        private FilterOperator operator;
        private String value;
        private LogicalOperator logicalOperator;
        private Integer orderIndex;
    }
}
