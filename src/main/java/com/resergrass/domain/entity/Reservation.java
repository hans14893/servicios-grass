package com.resergrass.domain.entity;

import com.resergrass.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservation_slot", columnList = "court_id,reservation_date,start_time,end_time")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_exact_slot", columnNames = {"court_id", "reservation_date", "start_time", "end_time"})
})
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(length = 120)
    private String guestName;

    @Column(length = 30)
    private String guestPhone;

    @ManyToOne(optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status = ReservationStatus.PENDIENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private OffsetDateTime paymentExpiresAt;

    private OffsetDateTime cancelledAt;

    @Column(length = 300)
    private String notes;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime updatedAt;

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
