package com.uab.salesprevision.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Creates a tenant company. A company must exist before any user can be assigned to it.")
public class CreateCompanyRequest {

    @Schema(description = "Company name, unique across the system.", example = "Acme Retail")
    @NotBlank(message = "{validation.company.name.required}")
    private String name;
}
