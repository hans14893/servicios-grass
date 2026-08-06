package com.resergrass.dto;

import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record ClientReservationSummaryDto(
        Long id,
        Long courtId,
        String courtName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        OffsetDateTime paymentExpiresAt
) {
}
