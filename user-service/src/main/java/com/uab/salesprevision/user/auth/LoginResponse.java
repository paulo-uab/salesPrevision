package com.uab.salesprevision.user.auth;

public record LoginResponse(String token, String type, long expiresInMs) {

    public LoginResponse(String token, long expiresInMs) {
        this(token, "Bearer", expiresInMs);
    }
}
