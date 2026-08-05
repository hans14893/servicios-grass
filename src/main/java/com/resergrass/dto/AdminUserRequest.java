package com.resergrass.dto;

import com.resergrass.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos") String phone,
        @NotBlank @Size(min = 6, max = 80) String password,
        @NotNull Role role
) {
}
