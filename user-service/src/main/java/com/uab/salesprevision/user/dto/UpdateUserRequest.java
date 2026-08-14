package com.uab.salesprevision.user.dto;

import com.uab.core.enums.ServiceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Replaces a user's company and role set. Username and password are not changed here.")
public class UpdateUserRequest {

    @Schema(description = "ID of the company this user belongs to.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.user.company.id.required}")
    private Long companyId;

    @Schema(description = "Per-service roles granted to this user — replaces the existing set entirely.")
    @Builder.Default
    private Set<ServiceRole> roles = new HashSet<>();
}
