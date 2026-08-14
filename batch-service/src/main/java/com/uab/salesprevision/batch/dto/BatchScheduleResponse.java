package com.uab.salesprevision.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "See POST /api/batch/schedules for the meaning of each field.")
public class BatchScheduleResponse {
    private Long id;
    private Long pipelineId;
    private String pipelineName;
    private String cronExpression;
    private Integer lookbackDays;
    private String predictionApiUrl;
    private Boolean active;
    private Boolean internalPrediction;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "When the scheduler last triggered this schedule, or null if it has never run.")
    private LocalDateTime lastRunAt;
}
