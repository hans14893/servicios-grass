package com.resergrass.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ReservationQuoteDto(
        Long courtId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal totalAmount,
        List<PriceBreakdownItemDto> breakdown
) {
}
