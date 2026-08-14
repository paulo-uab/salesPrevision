package com.uab.salesprevision.ingestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "One validation failure for a specific field/row — errorType matches the rule that failed (NOT_NULL, REGEX, MIN, MAX, ENUM, DATE_FORMAT, TYPE_CONVERSION).")
public class IngestionErrorResponse {
    private Long id;
    private Long ingestionJobId;
    private Long recordId;
    private String fieldName;
    private String errorType;
    private String message;
    private String rawValue;
    private Long lineNumber;
    private LocalDateTime createdAt;
}
