package com.resergrass.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos") String code,
        @NotBlank
        @Size(min = 8, max = 80)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,80}$", message = "La contraseña debe incluir mayúscula, minúscula y número")
        String newPassword
) {
}
