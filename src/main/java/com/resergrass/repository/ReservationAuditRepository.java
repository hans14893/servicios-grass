package com.resergrass.repository;

import com.resergrass.domain.entity.ReservationAudit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationAuditRepository extends JpaRepository<ReservationAudit, Long> {
    @EntityGraph(attributePaths = {"changedBy"})
    List<ReservationAudit> findByReservationIdOrderByChangedAtDesc(Long reservationId);
}
