package com.resergrass.dto;

import com.resergrass.domain.enums.PriceDayType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record CourtPriceRuleDto(
        Long id,
        Long courtId,
        PriceDayType dayType,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal hourlyPrice,
        BigDecimal halfHourPrice,
        boolean active
) {
}
