package com.uab.salesprevision.batch.dto;

import com.uab.core.enums.BatchExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "One concrete run of a schedule — either fired by its cron expression or triggered manually.")
public class BatchExecutionResponse {
    private Long id;
    private Long scheduleConfigId;
    private Long springBatchJobExecutionId;
    private BatchExecutionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long recordsSent;
    private Long recordsFailed;
    private String errorMessage;
    private LocalDateTime createdAt;
}
