package com.uab.salesprevision.user.dto;

import com.uab.core.enums.ServiceRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotNull
    private Long companyId;

    @Builder.Default
    private Set<ServiceRole> roles = new HashSet<>();
}
