package com.resergrass.dto;

import com.resergrass.domain.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull PaymentStatus status,
        @NotNull @DecimalMin("0.0") BigDecimal paidAmount,
        String method,
        String rejectionReason,
        String operationNumber
) {
}
