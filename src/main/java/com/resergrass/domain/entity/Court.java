package com.resergrass.domain.entity;

import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.CourtType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "courts", indexes = {
        @Index(name = "idx_courts_status", columnList = "status"),
        @Index(name = "idx_courts_type", columnList = "type")
})
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 40)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String mainImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourtType type = CourtType.GRASS_SINTETICO;

    @Column(length = 80)
    private String dimensions;

    @Column(nullable = false)
    private int maxPlayers = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourtStatus status = CourtStatus.DISPONIBLE;

    @Column(nullable = false)
    private boolean active = true;
}
