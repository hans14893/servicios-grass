package com.resergrass.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationRequest(
        Long clientId,
        @Size(max = 120) String guestName,
        @Size(max = 30) String guestPhone,
        @NotNull Long courtId,
        @NotNull LocalDate reservationDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        BigDecimal advanceAmount,
        @Size(max = 50) String paymentMethod,
        @Size(max = 300) String notes
) {
}
