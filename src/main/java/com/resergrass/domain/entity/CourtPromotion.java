package com.resergrass.domain.entity;

import com.resergrass.domain.enums.PromotionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "court_promotions", indexes = {
        @Index(name = "idx_promotions_court", columnList = "court_id"),
        @Index(name = "idx_promotions_dates", columnList = "valid_from,valid_to")
})
public class CourtPromotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    @Column(precision = 10, scale = 2)
    private BigDecimal fixedPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;

    private Integer requiredHours;

    private LocalTime startTime;

    private LocalTime endTime;

    private LocalDate validFrom;

    private LocalDate validTo;

    @Column(nullable = false)
    private boolean active = true;
}
