package com.uab.salesprevision.user.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A signed JWT (RS256) carrying the user's roles and companyId claims — use it as a Bearer token on every subsequent request.")
public record LoginResponse(
        @Schema(description = "The signed JWT. Its public key is published at GET /.well-known/jwks.json for other services to verify it.") String token,
        @Schema(description = "Always \"Bearer\" — the Authorization header scheme to use.") String type,
        @Schema(description = "Milliseconds until the token expires from the moment it was issued.") long expiresInMs
) {

    public LoginResponse(String token, long expiresInMs) {
        this(token, "Bearer", expiresInMs);
    }
}
