package com.uab.salesprevision.pipeline.dto;

import com.uab.core.enums.FilterOperator;
import com.uab.core.enums.ForecastFieldRole;
import com.uab.core.enums.LogicalOperator;
import com.uab.core.enums.TransformationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = """
        Creates a forecast pipeline: defines which template fields to extract, how \
        to transform them, and how the prediction-service should forecast from them. \
        Must have exactly one field with forecastRole=DATE and at least one with \
        forecastRole=TARGET, or creation fails with 400.""")
public class CreateForecastPipelineRequest {

    @Schema(description = "ID of the ingestion template this pipeline reads data from.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.pipeline.template.id.required}")
    private Long templateId;

    @Schema(description = "Pipeline name, unique within the company.", example = "Weekly sales forecast by category")
    @NotBlank(message = "{validation.pipeline.name.required}")
    @Size(max = 150, message = "{validation.pipeline.name.size}")
    private String name;

    @Schema(description = "Free-text description of this pipeline's purpose.")
    @Size(max = 1000, message = "{validation.pipeline.description.size}")
    private String description;

    @Schema(description = "Inactive pipelines are not used by any batch-service schedule.")
    private Boolean active = true;

    @Schema(description = """
            Model used for the "real" forecast. Valid values: naive, mean, drift, \
            seasonal_naive, ses, holtwinters, arima, linear_regression, random_forest, \
            xgboost — must match these exact literals, validated on the prediction-service \
            side (there is deliberately no shared enum between Java and Python here, to \
            avoid serialization mismatches between the two languages).""",
            example = "holtwinters")
    @NotBlank(message = "{validation.pipeline.forecast.model.required}")
    private String forecastModel = "naive";

    @Schema(description = """
            Control/baseline model — always runs in parallel to forecastModel on the same \
            data, giving a baseline-vs-chosen-model comparison on every single forecast \
            (this is what guarantees the experimental evaluation academically required). \
            Same valid values as forecastModel. If equal to forecastModel, it is only computed once.""",
            example = "naive")
    @NotBlank(message = "{validation.pipeline.control.model.required}")
    private String controlModel = "naive";

    @Schema(description = """
            Temporal granularity of the forecast: D (daily), W (weekly), ME (monthly), \
            QE (quarterly), YE (yearly). Determines how records are aggregated before \
            any model is fitted.""",
            example = "ME")
    @NotBlank(message = "{validation.pipeline.frequency.required}")
    private String frequency = "ME";

    @Schema(description = "How many future periods to forecast, in the unit defined by frequency (e.g. frequency=W, forecastHorizon=4 → 4 weeks ahead).")
    @Positive(message = "{validation.pipeline.horizon.positive}")
    private Integer forecastHorizon = 12;

    @Schema(description = """
            Seasonal cycle length (e.g. 12 for monthly with yearly seasonality, 52 for \
            weekly). Optional — inferred from frequency when omitted. Only relevant for \
            seasonal_naive and holtwinters; both require at least this many training \
            observations (holtwinters needs double that to detect seasonality — with less, \
            it gracefully degrades to trend-only, no seasonality).""")
    private Integer seasonPeriod;

    @Schema(description = "ARIMA (p, d, q) order as CSV, e.g. \"1,1,1\". Only used when forecastModel or controlModel is \"arima\"; ignored otherwise.", example = "1,1,1")
    private String arimaOrder;

    @Schema(description = "Number of past values (lags) used as features by the ML models (linear_regression, random_forest, xgboost). Ignored by the other models.")
    @Positive(message = "{validation.pipeline.n.lags.positive}")
    private Integer nLags = 12;

    @Schema(description = """
            When true, the ML models receive month/quarter as features, plus \
            day-of-week/weekend when frequency=D (for coarser granularities, "day of \
            week" would always be the same constant — not informative, so it is not added).""")
    private Boolean includeDateFeatures = true;

    @Schema(description = """
            Only has a real effect for arima and xgboost, which know how to reuse the \
            previous execution's state instead of refitting from scratch (cheaper \
            computationally, not necessarily more accurate). Silently ignored for the \
            other models. Does not combine with exogenous variables — if any field has \
            forecastRole=EXOG, the model always fits from scratch.""")
    private Boolean incrementalTraining = false;

    @Schema(description = "Fields extracted from the normalized record — defines what the date is, the target(s), the grouping, and the exogenous variables for this forecast.")
    @Valid
    @NotNull(message = "{validation.pipeline.fields.required}")
    @Size(min = 1, message = "{validation.pipeline.fields.required}")
    private List<FieldRequest> fields = new ArrayList<>();

    @Schema(description = "Filters applied to each record before it enters the forecast (e.g. exclude irrelevant categories).")
    @Valid
    private List<FilterRequest> filters = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "A field from the normalizedPayload, with its role in the forecast.")
    public static class FieldRequest {

        @Schema(description = "Field name as it exists in ingestion-service's normalizedPayload.", example = "Total Amount")
        @NotBlank(message = "{validation.pipeline.field.source.required}")
        private String sourceFieldName;

        @Schema(description = "Output name — this is the name used in the data sent to the prediction-service, and what should match date_field/target_fields/group_field on that side.", example = "total_amount")
        @NotBlank(message = "{validation.pipeline.field.target.required}")
        private String targetFieldName;

        @Schema(description = "Transformation applied to the value before sending (e.g. SCALE to convert units, DATE_FORMAT to normalize dates).")
        private TransformationType transformationType = TransformationType.NONE;

        @Schema(description = "Transformation parameters as JSON — the shape depends on transformationType (e.g. {\"factor\": 0.01} for SCALE).")
        private String transformationConfig;

        private Integer positionIndex;

        private Boolean active = true;

        @Schema(description = """
                This field's role in the forecast:
                • DATE — exactly one field, required. The temporal dimension.
                • TARGET — at least one, required. What gets forecast.
                • GROUP — at most one, optional. Segments the data into independent series \
                (e.g. one series per product category).
                • EXOG — zero or more, optional. Exogenous variable: never forecast, only \
                used as extra context by the models that support it (arima, \
                linear_regression, random_forest, xgboost) — the rest silently ignore it.
                • NONE — default. Field is extracted but not used in the forecast.""",
                example = "TARGET")
        private ForecastFieldRole forecastRole = ForecastFieldRole.NONE;

        @Schema(description = """
                How to aggregate multiple records in the same period — only relevant when \
                forecastRole is TARGET or EXOG (ignored for DATE/GROUP/NONE). Valid values: \
                sum, mean, last, max, min. E.g. "sum" for weekly revenue, "max" for a \
                promotion flag (if any record in the period had a promotion, the whole period counts as promotional).""",
                example = "sum")
        private String aggregation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "A filter applied to each record — combines with the next filter via logicalOperator.")
    public static class FilterRequest {

        @Schema(description = "Name of the field (in the normalizedPayload) to evaluate.", example = "Product Category")
        @NotBlank(message = "{validation.pipeline.filter.field.required}")
        private String fieldName;

        @Schema(description = "Comparison operator.", example = "EQ")
        @NotNull(message = "{validation.pipeline.filter.operator.required}")
        private FilterOperator operator;

        @Schema(description = "Comparison value. IN/NOT_IN use a comma-separated list; BETWEEN uses \"min,max\".", example = "Electronics")
        private String value;

        @Schema(description = "How this filter combines with the next one (evaluated in orderIndex order).")
        private LogicalOperator logicalOperator = LogicalOperator.AND;

        private Integer orderIndex;
    }
}
