package com.uab.salesprevision.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyRequest {

    @NotBlank
    private String name;
}
