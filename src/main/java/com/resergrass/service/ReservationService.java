package com.resergrass.service;

import com.resergrass.domain.entity.Payment;
import com.resergrass.domain.entity.Reservation;
import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.domain.enums.PaymentStatus;
import com.resergrass.domain.enums.ReservationStatus;
import com.resergrass.domain.enums.Role;
import com.resergrass.dto.PaymentRequest;
import com.resergrass.dto.ReportDto;
import com.resergrass.dto.ReservationDto;
import com.resergrass.dto.ReservationRequest;
import com.resergrass.exception.ApiException;
import com.resergrass.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final AvailableScheduleRepository scheduleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CourtPricingService pricingService;
    private final PaymentConfigService paymentConfigService;

    public List<ReservationDto> byCourtAndDate(Long courtId, LocalDate date) {
        var reservations = reservationRepository.findByCourtIdAndReservationDateOrderByStartTimeAsc(courtId, date)
                .stream().map(this::toDto).toList();
        log.info("RESERVATION_LIST courtId={} date={} count={}", courtId, date, reservations.size());
        return reservations;
    }

    public List<ReservationDto> mine(User user) {
        var reservations = reservationRepository.findByClientUserIdOrderByReservationDateDescStartTimeAsc(user.getId())
                .stream().map(this::toDto).toList();
        log.info("RESERVATION_LIST_MINE userId={} count={}", user.getId(), reservations.size());
        return reservations;
    }
    @Transactional
    public ReservationDto create(ReservationRequest request, User actor) {
        log.info("RESERVATION_CREATE_REQUEST courtId={} date={} start={} end={} actorId={} actorEmail={}",
                request.courtId(), request.reservationDate(), request.startTime(), request.endTime(), actor.getId(), actor.getEmail());
        validateRange(request);
        var court = courtRepository.findLockedById(request.courtId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cancha no encontrada"));
        if (!court.isActive() || court.getStatus() == CourtStatus.DESHABILITADA) {
            log.warn("RESERVATION_CREATE_REJECTED_DISABLED courtId={} status={} active={}", court.getId(), court.getStatus(), court.isActive());
            throw new ApiException(HttpStatus.BAD_REQUEST, "La cancha está deshabilitada");
        }
        if (court.getStatus() == CourtStatus.MANTENIMIENTO) {
            log.warn("RESERVATION_CREATE_REJECTED_MAINTENANCE courtId={}", court.getId());
            throw new ApiException(HttpStatus.CONFLICT, "La cancha está en mantenimiento");
        }
        var actorClient = clientRepository.findByUserEmail(actor.getEmail()).orElse(null);
        var client = request.clientId() != null
                ? clientRepository.findById(request.clientId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cliente no encontrado"))
                : actor.getRole() == Role.CLIENTE ? actorClient : null;
        var guestName = normalize(request.guestName());
        var guestPhone = normalize(request.guestPhone());

        if (client == null && actor.getRole() == Role.CLIENTE) {
            log.warn("RESERVATION_CREATE_REJECTED_NO_CLIENT actorId={} courtId={}", actor.getId(), court.getId());
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se encontró el cliente asociado a tu cuenta");
        }
        if (client == null && guestName == null) {
            log.warn("RESERVATION_CREATE_REJECTED_NO_GUEST actorId={} courtId={}", actor.getId(), court.getId());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debe indicar el nombre del cliente o visitante");
        }
        if (actor.getRole() == Role.CLIENTE && (actorClient == null || !client.getId().equals(actorClient.getId()))) {
            log.warn("RESERVATION_CREATE_REJECTED_FORBIDDEN actorId={} clientId={}", actor.getId(), client.getId());
            throw new ApiException(HttpStatus.FORBIDDEN, "Un cliente solo puede reservar para su propia cuenta");
        }

        ensureInsideConfiguredSchedule(request);
        ensureAvailable(request);

        var reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setClient(client);
        reservation.setGuestName(client == null ? guestName : null);
        reservation.setGuestPhone(client == null ? guestPhone : null);
        reservation.setCreatedBy(actor);
        reservation.setReservationDate(request.reservationDate());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setNotes(request.notes());
        reservation.setTotalAmount(pricingService.calculatePrice(court.getId(), request.reservationDate(), request.startTime(), request.endTime()));
        reservation.setPaymentExpiresAt(OffsetDateTime.now().plusMinutes(paymentConfigService.timeoutMinutes()));
        var saved = reservationRepository.save(reservation);

        var payment = new Payment();
        payment.setReservation(saved);
        payment.setStatus(PaymentStatus.PENDIENTE_PAGO);
        paymentRepository.save(payment);
        publishAvailability(saved);
        log.info("RESERVATION_CREATE_SUCCESS id={} courtId={} clientId={} guestName={} total={} status={}",
                saved.getId(), court.getId(), client == null ? null : client.getId(), saved.getGuestName(), saved.getTotalAmount(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public ReservationDto updateStatus(Long id, ReservationStatus status) {
        log.info("RESERVATION_STATUS_REQUEST id={} status={}", id, status);
        var reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        reservation.setStatus(status);
        var saved = reservationRepository.save(reservation);
        publishAvailability(saved);
        log.info("RESERVATION_STATUS_SUCCESS id={} courtId={} status={}", saved.getId(), saved.getCourt().getId(), saved.getStatus());
        return toDto(saved);
    }

    @Transactional
    public ReservationDto updatePayment(Long reservationId, PaymentRequest request) {
        log.info("PAYMENT_UPDATE_REQUEST reservationId={} status={} amount={} method={}", reservationId, request.status(), request.paidAmount(), request.method());
        var payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        payment.setStatus(request.status());
        payment.setPaidAmount(request.paidAmount());
        payment.setMethod(request.method());
        payment.setRejectionReason(request.rejectionReason());
        payment.setOperationNumber(request.operationNumber());
        if (request.status() == PaymentStatus.PAGADO) {
            payment.setPaidAt(java.time.OffsetDateTime.now());
            payment.getReservation().setStatus(ReservationStatus.CONFIRMADA);
        }
        if (request.status() == PaymentStatus.PAGO_EN_LOCAL) {
            payment.getReservation().setStatus(ReservationStatus.PENDIENTE);
        }
        if (request.status() == PaymentStatus.RECHAZADO) {
            payment.getReservation().setStatus(ReservationStatus.PENDIENTE);
        }
        paymentRepository.save(payment);
        reservationRepository.save(payment.getReservation());
        publishAvailability(payment.getReservation());
        log.info("PAYMENT_UPDATE_SUCCESS reservationId={} status={} amount={}", reservationId, payment.getStatus(), payment.getPaidAmount());
        return toDto(payment.getReservation());
    }

    @Transactional
    public ReservationDto confirmPayment(Long reservationId, String method) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        var payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        payment.setStatus(PaymentStatus.PAGADO);
        payment.setPaidAmount(reservation.getTotalAmount());
        payment.setMethod(method == null || method.isBlank() ? payment.getMethod() : method);
        payment.setPaidAt(OffsetDateTime.now());
        reservation.setStatus(ReservationStatus.CONFIRMADA);
        paymentRepository.save(payment);
        reservationRepository.save(reservation);
        publishAvailability(reservation);
        log.info("PAYMENT_CONFIRM_SUCCESS reservationId={} method={}", reservationId, payment.getMethod());
        return toDto(reservation);
    }

    @Transactional
    public ReservationDto rejectPayment(Long reservationId, String reason) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        var payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        payment.setStatus(PaymentStatus.RECHAZADO);
        payment.setRejectionReason(reason);
        reservation.setStatus(ReservationStatus.PENDIENTE);
        paymentRepository.save(payment);
        reservationRepository.save(reservation);
        publishAvailability(reservation);
        log.warn("PAYMENT_REJECTED reservationId={} reason={}", reservationId, reason);
        return toDto(reservation);
    }

    @Transactional
    public ReservationDto markPayAtVenue(Long reservationId) {
        var reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
        var payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        payment.setStatus(PaymentStatus.PAGO_EN_LOCAL);
        payment.setMethod("PAGO_EN_LOCAL");
        paymentRepository.save(payment);
        publishAvailability(reservation);
        log.info("PAYMENT_MARK_LOCAL reservationId={}", reservationId);
        return toDto(reservation);
    }

    public ReportDto report(LocalDate from, LocalDate to) {
        var reservations = reservationRepository.findByReservationDateBetweenOrderByReservationDateAscStartTimeAsc(from, to);
        var totalIncome = reservations.stream()
                .filter(r -> paymentRepository.findByReservationId(r.getId()).map(p -> p.getStatus() == PaymentStatus.PAGADO).orElse(false))
                .map(Reservation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReportDto(
                reservations.size(),
                reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CONFIRMADA).count(),
                reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELADA).count(),
                totalIncome
        );
    }

    private void validateRange(ReservationRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La hora de inicio debe ser menor que la hora fin");
        }
        var reservationStart = LocalDateTime.of(request.reservationDate(), request.startTime());
        if (!reservationStart.isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se puede reservar una fecha u hora pasada");
        }
    }

    private void ensureAvailable(ReservationRequest request) {
        var busy = reservationRepository.existsOverlappingReservation(
                request.courtId(),
                request.reservationDate(),
                request.startTime(),
                request.endTime(),
                List.of(ReservationStatus.PENDIENTE, ReservationStatus.CONFIRMADA)
        );
        if (busy) {
            log.warn("RESERVATION_CREATE_REJECTED_BUSY courtId={} date={} start={} end={}", request.courtId(), request.reservationDate(), request.startTime(), request.endTime());
            throw new ApiException(HttpStatus.CONFLICT, "El horario ya está reservado o pendiente");
        }
    }

    private void ensureInsideConfiguredSchedule(ReservationRequest request) {
        var schedules = scheduleRepository.findByCourtIdAndDayOfWeekAndActiveTrue(request.courtId(), request.reservationDate().getDayOfWeek());
        var valid = schedules.stream().anyMatch(schedule ->
                !request.startTime().isBefore(schedule.getStartTime())
                        && !request.endTime().isAfter(schedule.getEndTime())
        );
        if (!valid) {
            log.warn("RESERVATION_CREATE_REJECTED_OUT_OF_SCHEDULE courtId={} date={} start={} end={}", request.courtId(), request.reservationDate(), request.startTime(), request.endTime());
            throw new ApiException(HttpStatus.BAD_REQUEST, "El horario no está configurado como disponible para esta cancha");
        }
    }

    private ReservationDto toDto(Reservation reservation) {
        var payment = paymentRepository.findByReservationId(reservation.getId()).orElse(null);
        var paymentStatus = payment == null ? PaymentStatus.PENDIENTE_PAGO : payment.getStatus();
        var client = reservation.getClient();
        return new ReservationDto(
                reservation.getId(),
                client == null ? null : client.getId(),
                client == null ? reservation.getGuestName() : client.getUser().getFullName(),
                client == null ? reservation.getGuestPhone() : client.getUser().getPhone(),
                reservation.getGuestPhone(),
                reservation.getCourt().getId(),
                reservation.getCourt().getName(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getTotalAmount(),
                paymentStatus,
                reservation.getPaymentExpiresAt(),
                payment == null ? null : payment.getMethod(),
                payment == null ? null : payment.getRejectionReason(),
                reservation.getNotes()
        );
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cancelExpiredPendingPayments() {
        var now = OffsetDateTime.now();
        var reservations = reservationRepository.findByStatusAndPaymentExpiresAtBefore(ReservationStatus.PENDIENTE, now);
        reservations.forEach(reservation -> {
            var paymentStatus = paymentRepository.findByReservationId(reservation.getId()).map(Payment::getStatus).orElse(PaymentStatus.PENDIENTE_PAGO);
            if (paymentStatus == PaymentStatus.PENDIENTE_PAGO || paymentStatus == PaymentStatus.RECHAZADO) {
                reservation.setStatus(ReservationStatus.CANCELADA);
                reservation.setCancelledAt(now);
                reservationRepository.save(reservation);
                publishAvailability(reservation);
                log.warn("RESERVATION_AUTO_CANCELLED_EXPIRED id={} courtId={} expiresAt={}", reservation.getId(), reservation.getCourt().getId(), reservation.getPaymentExpiresAt());
            }
        });
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void publishAvailability(Reservation reservation) {
        messagingTemplate.convertAndSend(
                "/topic/availability/" + reservation.getCourt().getId() + "/" + reservation.getReservationDate(),
                toDto(reservation)
        );
        log.info("WEBSOCKET_AVAILABILITY_PUBLISHED courtId={} date={} reservationId={} status={}",
                reservation.getCourt().getId(), reservation.getReservationDate(), reservation.getId(), reservation.getStatus());
    }
}
