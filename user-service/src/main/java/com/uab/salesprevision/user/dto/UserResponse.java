package com.uab.salesprevision.user.dto;

import com.uab.core.enums.ServiceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A user account. Never includes the password hash.")
public class UserResponse {
    private Long id;
    private String username;
    private Long companyId;
    private String companyName;

    @Schema(description = "Per-service roles granted to this user.")
    @Builder.Default
    private Set<ServiceRole> roles = new HashSet<>();

    private Boolean active;
}
