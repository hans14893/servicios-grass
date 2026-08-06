package com.resergrass.dto;

import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

public record OperationsReservationDto(
        Long id,
        Long courtId,
        String courtName,
        String clientName,
        String clientPhone,
        LocalTime startTime,
        LocalTime endTime,
        ReservationStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount
) {
}
