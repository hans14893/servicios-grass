package com.resergrass.domain.entity;

import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "reservation_audits", indexes = {
        @Index(name = "idx_reservation_audit_reservation", columnList = "reservation_id,changed_at")
})
public class ReservationAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    private ReservationStatus previousReservationStatus;

    @Enumerated(EnumType.STRING)
    private ReservationStatus newReservationStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus previousPaymentStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus newPaymentStatus;

    @Column(length = 300)
    private String reason;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();
}
