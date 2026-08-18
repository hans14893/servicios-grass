package com.resergrass.dto;

import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record ReservationDto(
        Long id,
        Long clientId,
        String clientName,
        String contactPhone,
        String guestPhone,
        Long courtId,
        String courtName,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        PaymentStatus paymentStatus,
        OffsetDateTime paymentExpiresAt,
        String paymentMethod,
        String paymentRejectionReason,
        String notes
) {
}
