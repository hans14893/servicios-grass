package com.resergrass.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 6, max = 80) String password,
        @Size(max = 30) String phone,
        @Size(max = 20) String documentNumber,
        @Size(max = 200) String address
) {
}
