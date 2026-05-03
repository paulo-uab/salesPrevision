package com.uab.salesprevision.batch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class BatchScheduleResponse {
    private Long id;
    private Long pipelineId;
    private String pipelineName;
    private String cronExpression;
    private Integer lookbackDays;
    private String predictionApiUrl;
    private Boolean active;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
