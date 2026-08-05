package com.resergrass.domain.entity;

import com.resergrass.domain.enums.PriceDayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "court_price_rules", indexes = {
        @Index(name = "idx_price_rules_court", columnList = "court_id"),
        @Index(name = "idx_price_rules_lookup", columnList = "court_id,day_type,day_of_week,start_time,end_time")
})
public class CourtPriceRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private PriceDayType dayType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 20)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal halfHourPrice;

    @Column(nullable = false)
    private boolean active = true;
}
