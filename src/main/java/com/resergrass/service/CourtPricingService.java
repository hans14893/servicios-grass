package com.resergrass.service;

import com.resergrass.domain.entity.CourtPriceRule;
import com.resergrass.domain.enums.PriceDayType;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.CourtPriceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class CourtPricingService {
    private final CourtPriceRuleRepository priceRuleRepository;

    public BigDecimal calculatePrice(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        var rule = findRule(courtId, date, startTime, endTime);
        var minutes = Math.max(30, ChronoUnit.MINUTES.between(startTime, endTime));
        if (minutes == 30 && rule.getHalfHourPrice() != null) {
            return rule.getHalfHourPrice();
        }
        var hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return rule.getHourlyPrice().multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal displayHourlyPrice(Long courtId) {
        return priceRuleRepository.findByCourtIdAndActiveTrueOrderByStartTimeAsc(courtId).stream()
                .map(CourtPriceRule::getHourlyPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal displayHalfHourPrice(Long courtId) {
        return priceRuleRepository.findByCourtIdAndActiveTrueOrderByStartTimeAsc(courtId).stream()
                .map(CourtPriceRule::getHalfHourPrice)
                .filter(price -> price != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private CourtPriceRule findRule(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        var dayType = isWeekend(date.getDayOfWeek()) ? PriceDayType.WEEKEND : PriceDayType.WEEKDAY;
        return priceRuleRepository.findByCourtIdAndActiveTrueOrderByStartTimeAsc(courtId).stream()
                .filter(rule -> matchesDay(rule, dayType, date.getDayOfWeek()))
                .filter(rule -> !startTime.isBefore(rule.getStartTime()) && !endTime.isAfter(rule.getEndTime()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No existe tarifa configurada para este horario"));
    }

    private boolean matchesDay(CourtPriceRule rule, PriceDayType dayType, DayOfWeek dayOfWeek) {
        if (rule.getDayType() == PriceDayType.SPECIFIC_DAY) {
            return rule.getDayOfWeek() == dayOfWeek;
        }
        return rule.getDayType() == dayType;
    }

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
