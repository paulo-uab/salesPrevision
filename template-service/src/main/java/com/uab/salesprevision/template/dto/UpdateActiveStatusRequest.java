package com.uab.salesprevision.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Activates or deactivates a template without changing its definition. Inactive templates cannot be used for new ingestion jobs.")
public class UpdateActiveStatusRequest {

    @Schema(description = "New active status.", example = "false")
    @NotNull(message = "{validation.template.active.required}")
    private Boolean active;
}
