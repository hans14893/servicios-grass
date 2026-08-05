package com.resergrass.dto;

import java.math.BigDecimal;

public record ReportDto(long totalReservations, long confirmedReservations, long cancelledReservations, BigDecimal totalIncome) {
}
