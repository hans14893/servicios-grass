package com.resergrass.dto;

import com.resergrass.domain.enums.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CourtPromotionDto(
        Long id,
        Long courtId,
        String name,
        PromotionType type,
        BigDecimal fixedPrice,
        BigDecimal discountPercent,
        Integer requiredHours,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
