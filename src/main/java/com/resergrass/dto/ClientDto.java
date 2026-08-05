package com.resergrass.dto;

public record ClientDto(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phone,
        String documentNumber,
        String address
) {
}
