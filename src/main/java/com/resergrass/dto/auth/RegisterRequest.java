package com.resergrass.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 120) String fullName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 80) String password,
        @NotBlank @Pattern(regexp = "9\\d{8}", message = "El celular debe tener 9 dígitos y comenzar con 9") String phone,
        @Size(max = 20) String documentNumber,
        @Size(max = 200) String address
) {
}
