package com.resergrass.service;

import com.resergrass.domain.entity.AvailableSchedule;
import com.resergrass.domain.entity.Court;
import com.resergrass.domain.entity.Reservation;
import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;
import com.resergrass.dto.ClientCourtAvailabilityDto;
import com.resergrass.dto.ClientDashboardDto;
import com.resergrass.dto.ClientReservationSummaryDto;
import com.resergrass.repository.AvailableScheduleRepository;
import com.resergrass.repository.CourtRepository;
import com.resergrass.repository.PaymentRepository;
import com.resergrass.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientDashboardService {
    private final CourtRepository courtRepository;
    private final AvailableScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final CourtPricingService pricingService;

    @Transactional(readOnly = true)
    public ClientDashboardDto dashboard(Long userId) {
        var date = LocalDate.now();
        var now = LocalTime.now();
        var courts = courtRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(court -> court.getStatus() == CourtStatus.DISPONIBLE)
                .toList();
        var schedulesByCourt = scheduleRepository.findByDayOfWeekAndActiveTrue(date.getDayOfWeek()).stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getCourt().getId()));
        var reservationsByCourt = reservationRepository.findByReservationDateOrderByStartTimeAsc(date).stream()
                .filter(this::blocksAvailability)
                .collect(Collectors.groupingBy(reservation -> reservation.getCourt().getId()));

        var available = courts.stream()
                .map(court -> nextAvailable(court, date, now,
                        schedulesByCourt.getOrDefault(court.getId(), List.of()),
                        reservationsByCourt.getOrDefault(court.getId(), List.of())))
                .filter(item -> item != null)
                .toList();

        var nextReservation = reservationRepository.findUpcomingForUser(
                        userId, date, now, List.of(ReservationStatus.PENDIENTE, ReservationStatus.CONFIRMADA))
                .stream().findFirst().map(this::reservationSummary).orElse(null);
        return new ClientDashboardDto(date, available, nextReservation);
    }

    private ClientCourtAvailabilityDto nextAvailable(
            Court court,
            LocalDate date,
            LocalTime now,
            List<AvailableSchedule> schedules,
            List<Reservation> reservations
    ) {
        var ordered = schedules.stream().sorted(Comparator.comparing(AvailableSchedule::getStartTime)).toList();
        for (var schedule : ordered) {
            var cursor = schedule.getStartTime();
            while (cursor.isBefore(schedule.getEndTime())) {
                var end = cursor.plusHours(1);
                if (end.isAfter(schedule.getEndTime())) end = schedule.getEndTime();
                var start = cursor;
                var slotEnd = end;
                var busy = reservations.stream().anyMatch(item ->
                        item.getStartTime().isBefore(slotEnd) && item.getEndTime().isAfter(start));
                if (start.isAfter(now) && !busy) {
                    return new ClientCourtAvailabilityDto(
                            court.getId(), court.getName(), court.getDescription(), court.getMainImageUrl(),
                            start, slotEnd, pricingService.calculatePrice(court.getId(), date, start, slotEnd)
                    );
                }
                cursor = end;
            }
        }
        return null;
    }

    private boolean blocksAvailability(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.PENDIENTE
                || reservation.getStatus() == ReservationStatus.CONFIRMADA;
    }

    private ClientReservationSummaryDto reservationSummary(Reservation reservation) {
        var payment = paymentRepository.findByReservationId(reservation.getId()).orElse(null);
        return new ClientReservationSummaryDto(
                reservation.getId(),
                reservation.getCourt().getId(),
                reservation.getCourt().getName(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                payment == null ? PaymentStatus.PENDIENTE_PAGO : payment.getStatus(),
                reservation.getTotalAmount(),
                reservation.getPaymentExpiresAt()
        );
    }
}
