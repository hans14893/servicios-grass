package com.resergrass.dto.auth;

public record PasswordResetResponse(String message, int expiresInSeconds, int resendAfterSeconds) {
}
