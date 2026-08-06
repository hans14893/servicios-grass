package com.resergrass.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationsDashboardDto(
        LocalDate date,
        long totalReservations,
        long pendingReservations,
        long pendingPayments,
        long occupiedCourts,
        long totalCourts,
        BigDecimal collectedAmount,
        List<CourtOperationDto> courts,
        List<OperationsReservationDto> upcomingReservations
) {
}
