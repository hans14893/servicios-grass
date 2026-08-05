package com.resergrass.dto;

import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.CourtType;

import java.math.BigDecimal;
import java.util.List;

public record CourtDto(
        Long id,
        String name,
        String code,
        String description,
        String mainImageUrl,
        List<String> gallery,
        CourtType type,
        String dimensions,
        int maxPlayers,
        CourtStatus status,
        BigDecimal hourlyPrice,
        BigDecimal halfHourPrice,
        boolean active,
        List<CourtPriceRuleDto> priceRules,
        List<ScheduleDto> schedules,
        List<CourtPromotionDto> promotions
) {
}
