package com.resergrass.repository;

import com.resergrass.domain.entity.RefreshSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, Long> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshSession session set session.revoked = true, session.revokedAt = CURRENT_TIMESTAMP " +
            "where session.user.id = :userId and session.revoked = false")
    int revokeAllByUserId(Long userId);
}
