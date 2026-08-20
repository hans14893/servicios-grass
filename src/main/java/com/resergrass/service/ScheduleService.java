package com.resergrass.service;

import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.ReservationStatus;
import com.resergrass.domain.enums.Role;
import com.resergrass.domain.entity.User;
import com.resergrass.dto.CalendarSlotDto;
import com.resergrass.domain.entity.AvailableSchedule;
import com.resergrass.dto.ScheduleDto;
import com.resergrass.dto.ScheduleRequest;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.AvailableScheduleRepository;
import com.resergrass.repository.CourtRepository;
import com.resergrass.repository.ReservationRepository;
import com.resergrass.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {
    private final AvailableScheduleRepository scheduleRepository;
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final CourtPricingService pricingService;

    public List<ScheduleDto> byCourtAndDate(Long courtId, LocalDate date) {
        var schedules = scheduleRepository.findByCourtIdAndDayOfWeekAndActiveTrue(courtId, date.getDayOfWeek())
                .stream().map(this::toDto).toList();
        log.info("SCHEDULE_LIST courtId={} date={} day={} count={}", courtId, date, date.getDayOfWeek(), schedules.size());
        return schedules;
    }

    public List<CalendarSlotDto> calendar(Long courtId, LocalDate date, User actor) {
        log.info("CALENDAR_REQUEST courtId={} date={}", courtId, date);
        var court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada"));
        var schedules = scheduleRepository.findByCourtIdAndDayOfWeekAndActiveTrue(courtId, date.getDayOfWeek());
        var reservations = reservationRepository.findByCourtIdAndReservationDateOrderByStartTimeAsc(courtId, date);
        var slots = schedules.stream().flatMap(schedule -> {
            var scheduleSlots = new java.util.ArrayList<CalendarSlotDto>();
            var cursor = LocalDateTime.of(date, schedule.getStartTime());
            var scheduleEnd = LocalDateTime.of(date, schedule.getEndTime());
            if (!scheduleEnd.isAfter(cursor)) scheduleEnd = scheduleEnd.plusDays(1);
            while (cursor.isBefore(scheduleEnd)) {
                var next = cursor.plusHours(1);
                if (next.isAfter(scheduleEnd)) {
                    next = scheduleEnd;
                }
                var slotStartDateTime = cursor;
                var slotEndDateTime = next;
                var slotStart = cursor.toLocalTime();
                var slotEnd = next.toLocalTime();
                var reservation = reservations.stream()
                        .filter(item -> item.getStatus() == ReservationStatus.PENDIENTE
                                || item.getStatus() == ReservationStatus.CONFIRMADA)
                        .filter(item -> {
                            var reservationStart = LocalDateTime.of(date, item.getStartTime());
                            if (reservationStart.isBefore(LocalDateTime.of(date, schedule.getStartTime()))) {
                                reservationStart = reservationStart.plusDays(1);
                            }
                            var reservationEnd = LocalDateTime.of(reservationStart.toLocalDate(), item.getEndTime());
                            if (!reservationEnd.isAfter(reservationStart)) reservationEnd = reservationEnd.plusDays(1);
                            return reservationStart.isBefore(slotEndDateTime) && reservationEnd.isAfter(slotStartDateTime);
                        })
                        .findFirst()
                        .orElse(null);
                var slotIsPast = !slotStartDateTime.isAfter(LocalDateTime.now());
                var status = slotIsPast
                        ? "NO_DISPONIBLE"
                        : court.getStatus() == CourtStatus.MANTENIMIENTO
                        ? "MANTENIMIENTO"
                        : reservation == null
                        ? "DISPONIBLE"
                        : reservation.getStatus() == ReservationStatus.PENDIENTE ? "PENDIENTE" : "RESERVADO";
                var canManage = actor != null && (actor.getRole() == Role.ADMIN || actor.getRole() == Role.PERSONAL);
                var client = reservation == null ? null : reservation.getClient();
                var reservationName = canManage && reservation != null
                        ? client == null ? reservation.getGuestName() : client.getUser().getFullName()
                        : null;
                var reservationPhone = canManage && reservation != null
                        ? client == null ? reservation.getGuestPhone() : client.getUser().getPhone()
                        : null;
                var payment = reservation == null ? null : paymentRepository.findByReservationId(reservation.getId()).orElse(null);
                var totalAmount = canManage && reservation != null ? reservation.getTotalAmount() : null;
                var paidAmount = canManage && payment != null ? payment.getPaidAmount() : null;
                var pendingAmount = canManage && reservation != null
                        ? reservation.getTotalAmount().subtract(payment == null ? java.math.BigDecimal.ZERO : payment.getPaidAmount())
                                .max(java.math.BigDecimal.ZERO)
                        : null;
                scheduleSlots.add(new CalendarSlotDto(
                        court.getId(),
                        court.getName(),
                        slotStart,
                        slotEnd,
                        status,
                        reservation == null ? null : reservation.getId(),
                        reservationName,
                        reservationPhone,
                        canManage && reservation != null ? reservation.getStartTime() : null,
                        canManage && reservation != null ? reservation.getEndTime() : null,
                        totalAmount,
                        paidAmount,
                        pendingAmount,
                        canManage && payment != null ? payment.getStatus().name() : null,
                        pricingService.calculatePrice(courtId, date, slotStart, slotEnd)
                ));
                cursor = next;
            }
            return scheduleSlots.stream();
        }).toList();
        log.info("CALENDAR_SUCCESS courtId={} date={} slots={} reservations={} courtStatus={}", courtId, date, slots.size(), reservations.size(), court.getStatus());
        return slots;
    }

    public ScheduleDto create(ScheduleRequest request) {
        log.info("SCHEDULE_CREATE_REQUEST courtId={} day={} start={} end={} active={}", request.courtId(), request.dayOfWeek(), request.startTime(), request.endTime(), request.active());
        validateTimes(request);
        var court = courtRepository.findById(request.courtId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada"));
        var schedule = new AvailableSchedule();
        schedule.setCourt(court);
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setActive(request.active());
        var saved = scheduleRepository.save(schedule);
        log.info("SCHEDULE_CREATE_SUCCESS id={} courtId={} day={}", saved.getId(), saved.getCourt().getId(), saved.getDayOfWeek());
        return toDto(saved);
    }

    private void validateTimes(ScheduleRequest request) {
        if (request.startTime().equals(request.endTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La hora de inicio y fin no pueden ser iguales");
        }
    }

    private ScheduleDto toDto(AvailableSchedule schedule) {
        return new ScheduleDto(
                schedule.getId(),
                schedule.getCourt().getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.isActive()
        );
    }
}
