package com.uab.salesprevision.user.dto;

import com.uab.core.enums.ServiceRole;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private Long companyId;
    private String companyName;

    @Builder.Default
    private Set<ServiceRole> roles = new HashSet<>();

    private Boolean active;
}
