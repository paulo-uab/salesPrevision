package com.uab.core.dto.ingestion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
