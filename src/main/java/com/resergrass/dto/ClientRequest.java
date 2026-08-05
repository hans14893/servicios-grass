package com.resergrass.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Email @Size(max = 160) String email,
        @Size(max = 30) String phone,
        @Size(max = 20) String documentNumber,
        @Size(max = 200) String address
) {
}
