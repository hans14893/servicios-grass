package com.resergrass.dto;

import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;

import java.time.OffsetDateTime;

public record ReservationAuditDto(
        Long id,
        String action,
        String changedBy,
        ReservationStatus previousReservationStatus,
        ReservationStatus newReservationStatus,
        PaymentStatus previousPaymentStatus,
        PaymentStatus newPaymentStatus,
        String reason,
        OffsetDateTime changedAt
) {
}
