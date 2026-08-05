package com.uab.salesprevision.ingestion.dto;

import com.uab.core.enums.IngestionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IngestionJobResponse {
    private Long id;
    private IngestionStatus status;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    private String storagePath;
    private String checksum;
    private Long templateId;
    private String templateName;
    private Long recordCount;
    private Long errorCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private String errorMessage;
}
