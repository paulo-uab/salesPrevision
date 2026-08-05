package com.uab.salesprevision.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateBatchScheduleRequest {

    @NotNull(message = "{validation.batch.pipeline.id.required}")
    private Long pipelineId;

    @NotBlank(message = "{validation.batch.cron.required}")
    @Size(max = 150, message = "{validation.batch.cron.size}")
    private String cronExpression;

    @Positive(message = "{validation.batch.lookback.days.positive}")
    private Integer lookbackDays = 7;

    @NotBlank(message = "{validation.batch.prediction.url.required}")
    @Size(max = 500, message = "{validation.batch.prediction.url.size}")
    private String predictionApiUrl;

    private Boolean active = true;

    @Size(max = 100, message = "{validation.batch.created.by.size}")
    private String createdBy;
}
