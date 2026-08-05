package com.resergrass.dto;

import com.resergrass.domain.enums.PriceDayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record CourtPriceRuleRequest(
        Long courtId,
        @NotNull PriceDayType dayType,
        DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @DecimalMin("0.0") BigDecimal hourlyPrice,
        @DecimalMin("0.0") BigDecimal halfHourPrice,
        boolean active
) {
}
