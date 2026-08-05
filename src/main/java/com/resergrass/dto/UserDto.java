package com.resergrass.dto;

import com.resergrass.domain.enums.Role;

public record UserDto(Long id, String fullName, String email, String phone, Role role, boolean enabled) {
}
