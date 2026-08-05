package com.resergrass.dto;

import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.CourtType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourtRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 40) String code,
        @Size(max = 500) String description,
        @Size(max = 500) String mainImageUrl,
        List<String> gallery,
        CourtType type,
        @Size(max = 80) String dimensions,
        @PositiveOrZero Integer maxPlayers,
        CourtStatus status,
        boolean active,
        List<CourtPriceRuleRequest> priceRules,
        List<ScheduleRequest> schedules,
        List<CourtPromotionRequest> promotions
) {
}
