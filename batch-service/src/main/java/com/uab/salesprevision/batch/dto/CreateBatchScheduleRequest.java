package com.uab.salesprevision.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Schedules recurring forecast runs: a pipeline, a cron expression, a lookback window, and where to send the results.")
public class CreateBatchScheduleRequest {

    @Schema(description = "ID of the pipeline to run on this schedule.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.batch.pipeline.id.required}")
    private Long pipelineId;

    @Schema(description = "Standard 6-part Spring cron expression controlling when this schedule fires.", example = "0 0 6 * * *")
    @NotBlank(message = "{validation.batch.cron.required}")
    @Size(max = 150, message = "{validation.batch.cron.size}")
    private String cronExpression;

    @Schema(description = "Only ingestion jobs created within this many days are read on each run — independent of how often the schedule itself fires.")
    @Positive(message = "{validation.batch.lookback.days.positive}")
    private Integer lookbackDays = 7;

    @Schema(description = "Full URL of the prediction API this schedule sends transformed records to. Can be any external URL, or the project's own prediction-service.", example = "http://localhost:8085/api/forecast")
    @NotBlank(message = "{validation.batch.prediction.url.required}")
    @Size(max = 500, message = "{validation.batch.prediction.url.size}")
    private String predictionApiUrl;

    @Schema(description = "Inactive schedules are skipped by the scheduler, even if their cron expression would otherwise fire.")
    private Boolean active = true;

    @Schema(description = """
            Set this to true only when predictionApiUrl points at this project's own \
            prediction-service. When true, the internal-service token is attached to the \
            outbound call so the prediction-service's authentication accepts it. Must stay \
            false for any genuinely external, user-configured prediction API — the token \
            must never be sent to a third party.""")
    private Boolean internalPrediction = false;

    @Schema(description = "Free-text identifier of who created this schedule, for audit purposes.")
    @Size(max = 100, message = "{validation.batch.created.by.size}")
    private String createdBy;
}
