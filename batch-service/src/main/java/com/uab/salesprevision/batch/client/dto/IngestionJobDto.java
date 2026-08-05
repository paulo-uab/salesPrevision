package com.uab.salesprevision.batch.client.dto;

import com.uab.core.enums.IngestionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class IngestionJobDto {
    private Long id;
    private IngestionStatus status;
    private Long templateId;
    private String templateName;
    private LocalDateTime createdAt;
}
