package com.uab.salesprevision.user.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Username/password credentials for a user seeded by DataSeeder or created via POST /api/users.")
public record LoginRequest(
        @Schema(description = "Username.", example = "admin") String username,
        @Schema(description = "Plain-text password, checked against the stored BCrypt hash.", example = "admin123") String password
) {}
