package com.uab.core.dto.ingestion;

import com.uab.core.enums.IngestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class CreateIngestionJobResponse {
    private Long jobId;
    private IngestionStatus status;
    private String originalFileName;
    private String storedFileName;
    private Long templateId;
    private String templateName;
    private LocalDateTime createdAt;
}
