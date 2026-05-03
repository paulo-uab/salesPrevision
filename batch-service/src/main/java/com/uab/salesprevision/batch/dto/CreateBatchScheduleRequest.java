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

    @NotNull(message = "O pipelineId é obrigatório")
    private Long pipelineId;

    @NotBlank(message = "A expressão cron é obrigatória")
    @Size(max = 150)
    private String cronExpression;

    @Positive(message = "O lookbackDays deve ser positivo")
    private Integer lookbackDays = 7;

    @NotBlank(message = "O URL da API de previsão é obrigatório")
    @Size(max = 500)
    private String predictionApiUrl;

    private Boolean active = true;

    @Size(max = 100)
    private String createdBy;
}
