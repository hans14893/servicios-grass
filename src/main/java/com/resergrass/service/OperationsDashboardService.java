package com.resergrass.service;

import com.resergrass.domain.entity.Payment;
import com.resergrass.domain.entity.Reservation;
import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;
import com.resergrass.dto.CourtOperationDto;
import com.resergrass.dto.OperationsDashboardDto;
import com.resergrass.dto.OperationsReservationDto;
import com.resergrass.repository.CourtRepository;
import com.resergrass.repository.PaymentRepository;
import com.resergrass.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationsDashboardService {
    private final CourtRepository courtRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public OperationsDashboardDto today() {
        var date = LocalDate.now();
        var now = LocalTime.now();
        var courts = courtRepository.findAll().stream()
                .sorted(Comparator.comparing(court -> court.getName().toLowerCase()))
                .toList();
        var reservations = reservationRepository.findByReservationDateOrderByStartTimeAsc(date);
        var paymentByReservation = paymentRepository.findByReservationReservationDate(date).stream()
                .collect(Collectors.toMap(payment -> payment.getReservation().getId(), Function.identity()));
        var active = reservations.stream()
                .filter(this::isActive)
                .toList();

        var courtStates = courts.stream().map(court -> {
            var courtReservations = active.stream()
                    .filter(item -> item.getCourt().getId().equals(court.getId()))
                    .toList();
            var current = courtReservations.stream()
                    .filter(item -> !item.getStartTime().isAfter(now) && item.getEndTime().isAfter(now))
                    .findFirst().orElse(null);
            var next = courtReservations.stream()
                    .filter(item -> item.getStartTime().isAfter(now))
                    .findFirst().orElse(null);
            var status = !court.isActive() || court.getStatus() == CourtStatus.DESHABILITADA ? "DESHABILITADA"
                    : court.getStatus() == CourtStatus.MANTENIMIENTO ? "MANTENIMIENTO"
                    : current != null ? "OCUPADA" : "LIBRE";
            return new CourtOperationDto(
                    court.getId(), court.getName(), status,
                    toDto(current, paymentByReservation),
                    toDto(next, paymentByReservation)
            );
        }).toList();

        var pendingPayments = active.stream()
                .filter(item -> paymentStatus(item, paymentByReservation) != PaymentStatus.PAGADO)
                .count();
        var collected = paymentByReservation.values().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAGADO)
                .map(Payment::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var upcoming = active.stream()
                .filter(item -> item.getEndTime().isAfter(now))
                .limit(8)
                .map(item -> toDto(item, paymentByReservation))
                .toList();

        return new OperationsDashboardDto(
                date,
                active.size(),
                active.stream().filter(item -> item.getStatus() == ReservationStatus.PENDIENTE).count(),
                pendingPayments,
                courtStates.stream().filter(item -> item.status().equals("OCUPADA")).count(),
                courts.size(),
                collected,
                courtStates,
                upcoming
        );
    }

    private boolean isActive(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.PENDIENTE
                || reservation.getStatus() == ReservationStatus.CONFIRMADA;
    }

    private PaymentStatus paymentStatus(Reservation reservation, Map<Long, Payment> paymentByReservation) {
        var payment = paymentByReservation.get(reservation.getId());
        return payment == null ? PaymentStatus.PENDIENTE_PAGO : payment.getStatus();
    }

    private OperationsReservationDto toDto(Reservation reservation, Map<Long, Payment> payments) {
        if (reservation == null) return null;
        var client = reservation.getClient();
        return new OperationsReservationDto(
                reservation.getId(),
                reservation.getCourt().getId(),
                reservation.getCourt().getName(),
                client == null ? reservation.getGuestName() : client.getUser().getFullName(),
                client == null ? reservation.getGuestPhone() : client.getUser().getPhone(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                paymentStatus(reservation, payments),
                reservation.getTotalAmount()
        );
    }
}
