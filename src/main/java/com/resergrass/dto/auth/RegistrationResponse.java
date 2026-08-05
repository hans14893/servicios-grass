package com.resergrass.dto.auth;

public record RegistrationResponse(
        String email,
        String message,
        int expiresInSeconds,
        int resendAfterSeconds
) {
}
