package com.uab.salesprevision.ingestion.dto;

import com.uab.core.enums.ValidationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "One ingested row, with both its original values and the type-converted, template-mapped result.")
public class IngestedRecordResponse {
    private Long id;
    private Long ingestionJobId;
    private Long recordIndex;
    private ValidationStatus validationStatus;

    @Schema(description = "Original values as read from the source file, before any conversion.")
    private String rawPayload;

    @Schema(description = "Values after type conversion and template field mapping — this is what pipelines and forecasts consume.")
    private String normalizedPayload;

    private LocalDateTime createdAt;
}
