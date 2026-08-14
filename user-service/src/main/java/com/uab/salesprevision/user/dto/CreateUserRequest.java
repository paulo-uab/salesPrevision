package com.uab.salesprevision.user.dto;

import com.uab.core.enums.ServiceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Creates a user account belonging to a company, with a set of per-service roles.")
public class CreateUserRequest {

    @Schema(description = "Login username, unique across the whole system.", example = "jane.doe")
    @NotBlank(message = "{validation.user.username.required}")
    private String username;

    @Schema(description = "Plain-text password — hashed with BCrypt before being stored, never returned in any response.")
    @NotBlank(message = "{validation.user.password.required}")
    private String password;

    @Schema(description = "ID of the company this user belongs to — determines which company's data (templates, pipelines, etc.) this user can see.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.user.company.id.required}")
    private Long companyId;

    @Schema(description = "Per-service roles granted to this user (e.g. TEMPLATE_READ, PIPELINE_EDIT). An \"admin\" is simply a user holding every role.")
    @Builder.Default
    private Set<ServiceRole> roles = new HashSet<>();
}
