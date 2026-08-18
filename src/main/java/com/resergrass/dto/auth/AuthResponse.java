package com.resergrass.dto.auth;

import com.resergrass.domain.enums.Role;

public record AuthResponse(
        String token,
        Long userId,
        String refreshToken,
        String fullName,
        String email,
        Role role
) {
}
