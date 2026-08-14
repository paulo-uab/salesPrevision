package com.uab.salesprevision.ingestion.dto;

import com.uab.core.enums.IngestionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "The job just created. If autoProcess was true, status may already reflect the outcome of processing.")
public class CreateIngestionJobResponse {
    private Long jobId;
    private IngestionStatus status;
    private String originalFileName;
    private String storedFileName;
    private Long templateId;
    private String templateName;
    private LocalDateTime createdAt;
}
