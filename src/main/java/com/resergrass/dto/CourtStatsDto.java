package com.resergrass.dto;

import java.math.BigDecimal;

public record CourtStatsDto(
        Long courtId,
        String courtName,
        long totalReservations,
        long confirmedReservations,
        long cancelledReservations,
        BigDecimal projectedIncome
) {
}
