package com.resergrass.service;

import com.resergrass.domain.entity.CourtPriceRule;
import com.resergrass.domain.enums.PriceDayType;
import com.resergrass.repository.CourtPriceRuleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourtPricingServiceTest {

    @Test
    void quotesEachHourWithItsMatchingRate() {
        var repository = mock(CourtPriceRuleRepository.class);
        when(repository.findByCourtIdAndActiveTrueOrderByStartTimeAsc(1L))
                .thenReturn(List.of(rule("18:00", "19:00", "60"), rule("19:00", "22:00", "80")));

        var quote = new CourtPricingService(repository).quote(
                1L, LocalDate.of(2026, 8, 6), LocalTime.of(18, 0), LocalTime.of(21, 0)
        );

        assertThat(quote.totalAmount()).isEqualByComparingTo("220.00");
        assertThat(quote.breakdown()).extracting(item -> item.amount())
                .containsExactly(new BigDecimal("60.00"), new BigDecimal("80.00"), new BigDecimal("80.00"));
    }

    @Test
    void quotesHoursThatContinueAfterMidnight() {
        var repository = mock(CourtPriceRuleRepository.class);
        when(repository.findByCourtIdAndActiveTrueOrderByStartTimeAsc(1L))
                .thenReturn(List.of(rule("17:00", "01:00", "70")));

        var quote = new CourtPricingService(repository).quote(
                1L, LocalDate.of(2026, 8, 19), LocalTime.of(23, 0), LocalTime.of(1, 0)
        );

        assertThat(quote.totalAmount()).isEqualByComparingTo("140.00");
        assertThat(quote.breakdown()).extracting(item -> item.startTime())
                .containsExactly(LocalTime.of(23, 0), LocalTime.MIDNIGHT);
        assertThat(quote.breakdown()).extracting(item -> item.endTime())
                .containsExactly(LocalTime.MIDNIGHT, LocalTime.of(1, 0));
    }

    private CourtPriceRule rule(String start, String end, String price) {
        var rule = new CourtPriceRule();
        rule.setDayType(PriceDayType.WEEKDAY);
        rule.setStartTime(LocalTime.parse(start));
        rule.setEndTime(LocalTime.parse(end));
        rule.setHourlyPrice(new BigDecimal(price));
        rule.setActive(true);
        return rule;
    }
}
