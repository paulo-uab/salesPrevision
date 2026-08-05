package com.uab.salesprevision.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserActiveStatusRequest {

    @NotNull
    private Boolean active;
}
