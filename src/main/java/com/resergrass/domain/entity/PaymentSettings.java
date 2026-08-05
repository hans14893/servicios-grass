package com.resergrass.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment_settings")
public class PaymentSettings {
    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, length = 30)
    private String yapePhoneNumber;

    @Column(nullable = false, length = 30)
    private String whatsappPhoneNumber;

    @Column(nullable = false, length = 500)
    private String yapeQrUrl;

    @Column(nullable = false)
    private int paymentTimeoutMinutes;

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    @PrePersist
    void touch() {
        updatedAt = OffsetDateTime.now();
    }
}
