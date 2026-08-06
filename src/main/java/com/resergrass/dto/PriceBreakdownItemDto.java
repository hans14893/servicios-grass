package com.resergrass.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record PriceBreakdownItemDto(
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal amount
) {
}
