package com.uab.core.serviceauth;

record LoginResponse(String token, String type, long expiresInMs) {
}
