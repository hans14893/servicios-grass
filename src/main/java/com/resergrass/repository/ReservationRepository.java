package com.resergrass.repository;

import com.resergrass.domain.entity.Reservation;
import com.resergrass.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
            select count(r) > 0 from Reservation r
            where r.court.id = :courtId
              and r.reservationDate = :date
              and r.status in :statuses
              and r.startTime < :endTime
              and r.endTime > :startTime
            """)
    boolean existsOverlappingReservation(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    List<Reservation> findByCourtIdAndReservationDateOrderByStartTimeAsc(Long courtId, LocalDate date);

    List<Reservation> findByClientUserIdOrderByReservationDateDescStartTimeAsc(Long userId);

    @EntityGraph(attributePaths = {"court"})
    @Query("""
            select r from Reservation r
            where r.client.user.id = :userId
              and (r.reservationDate > :date or (r.reservationDate = :date and r.endTime > :time))
              and r.status in :statuses
            order by r.reservationDate asc, r.startTime asc
            """)
    List<Reservation> findUpcomingForUser(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    List<Reservation> findByCourtIdOrderByReservationDateDescStartTimeAsc(Long courtId);

    List<Reservation> findByReservationDateBetweenOrderByReservationDateAscStartTimeAsc(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"court", "client", "client.user"})
    List<Reservation> findByReservationDateOrderByStartTimeAsc(LocalDate date);

    List<Reservation> findByStatusAndPaymentExpiresAtBefore(ReservationStatus status, OffsetDateTime now);
}
