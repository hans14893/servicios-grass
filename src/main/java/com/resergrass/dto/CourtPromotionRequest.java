package com.resergrass.dto;

import com.resergrass.domain.enums.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CourtPromotionRequest(
        Long courtId,
        @NotBlank @Size(max = 120) String name,
        @NotNull PromotionType type,
        @DecimalMin("0.0") BigDecimal fixedPrice,
        @DecimalMin("0.0") BigDecimal discountPercent,
        Integer requiredHours,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
