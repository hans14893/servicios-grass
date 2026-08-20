package com.resergrass.service;

import com.resergrass.domain.entity.CourtPriceRule;
import com.resergrass.domain.enums.PriceDayType;
import com.resergrass.dto.PriceBreakdownItemDto;
import com.resergrass.dto.ReservationQuoteDto;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.CourtPriceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class CourtPricingService {
    private final CourtPriceRuleRepository priceRuleRepository;

    public BigDecimal calculatePrice(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return quote(courtId, date, startTime, endTime).totalAmount();
    }

    public ReservationQuoteDto quote(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        var items = priceItems(courtId, date, startTime, endTime);
        var total = items.stream().map(PriceBreakdownItemDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new ReservationQuoteDto(courtId, date, startTime, endTime, total, items);
    }

    private ArrayList<PriceBreakdownItemDto> priceItems(Long courtId, LocalDate date, LocalTime start, LocalTime end) {
        if (start.equals(end)) throw new ApiException(HttpStatus.BAD_REQUEST, "Rango invalido");
        var items = new ArrayList<PriceBreakdownItemDto>();
        var cursor = LocalDateTime.of(date, start);
        var rangeEnd = LocalDateTime.of(date, end);
        if (!rangeEnd.isAfter(cursor)) rangeEnd = rangeEnd.plusDays(1);
        while (cursor.isBefore(rangeEnd)) {
            var rule = findRule(courtId, date, cursor.toLocalTime());
            var ruleEnd = LocalDateTime.of(cursor.toLocalDate(), rule.getEndTime());
            if (!ruleEnd.isAfter(cursor)) ruleEnd = ruleEnd.plusDays(1);
            var segmentEnd = cursor.plusHours(1);
            if (rangeEnd.isBefore(segmentEnd)) segmentEnd = rangeEnd;
            if (ruleEnd.isBefore(segmentEnd)) segmentEnd = ruleEnd;
            items.add(new PriceBreakdownItemDto(cursor.toLocalTime(), segmentEnd.toLocalTime(), segmentPrice(rule, cursor, segmentEnd)));
            cursor = segmentEnd;
        }
        return items;
    }

    private BigDecimal segmentPrice(CourtPriceRule rule, LocalDateTime start, LocalDateTime end) {
        var minutes = Math.max(30, ChronoUnit.MINUTES.between(start, end));
        if (minutes == 30 && rule.getHalfHourPrice() != null) {
            return rule.getHalfHourPrice().setScale(2, RoundingMode.HALF_UP);
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

    private CourtPriceRule findRule(Long courtId, LocalDate date, LocalTime time) {
        var dayType = isWeekend(date.getDayOfWeek()) ? PriceDayType.WEEKEND : PriceDayType.WEEKDAY;
        return priceRuleRepository.findByCourtIdAndActiveTrueOrderByStartTimeAsc(courtId).stream()
                .filter(rule -> matchesDay(rule, dayType, date.getDayOfWeek()))
                .filter(rule -> matchesTime(rule, time))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No existe tarifa configurada para este horario"));
    }

    private boolean matchesTime(CourtPriceRule rule, LocalTime time) {
        if (rule.getStartTime().isBefore(rule.getEndTime())) {
            return !time.isBefore(rule.getStartTime()) && time.isBefore(rule.getEndTime());
        }
        return !time.isBefore(rule.getStartTime()) || time.isBefore(rule.getEndTime());
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
