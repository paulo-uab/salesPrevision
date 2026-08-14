package com.uab.salesprevision.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A tenant company — the unit every other service's data (templates, pipelines, ingestion jobs, schedules) is isolated by.")
public class CompanyResponse {
    private Long id;
    private String name;
}
