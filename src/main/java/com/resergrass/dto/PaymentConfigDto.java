package com.resergrass.dto;

public record PaymentConfigDto(
        String ownerName,
        String yapePhoneNumber,
        String whatsappPhoneNumber,
        String yapeQrUrl,
        int paymentTimeoutMinutes
) {
}
