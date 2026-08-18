package com.resergrass.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "refresh_sessions", indexes = {
        @Index(name = "idx_refresh_session_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_session_user", columnList = "user_id")
})
public class RefreshSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime lastUsedAt;
    private OffsetDateTime revokedAt;
}
