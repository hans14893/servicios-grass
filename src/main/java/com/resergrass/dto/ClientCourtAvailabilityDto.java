package com.resergrass.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ClientCourtAvailabilityDto(
        Long courtId,
        String courtName,
        String description,
        String imageUrl,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price
) {
}
