package com.uab.salesprevision.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Activates or deactivates a user. Inactive users cannot log in — findByUsernameAndActiveTrue excludes them at authentication time.")
public class UpdateUserActiveStatusRequest {

    @Schema(description = "New active status.", example = "false")
    @NotNull(message = "{validation.user.active.required}")
    private Boolean active;
}
