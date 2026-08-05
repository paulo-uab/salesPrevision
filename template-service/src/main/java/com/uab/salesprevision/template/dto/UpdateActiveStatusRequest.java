package com.uab.salesprevision.template.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateActiveStatusRequest {

    @NotNull
    private Boolean active;
}
