package com.resergrass.controller;

import com.resergrass.domain.entity.User;
import com.resergrass.domain.enums.ReservationStatus;
import com.resergrass.dto.PaymentRequest;
import com.resergrass.dto.ReportDto;
import com.resergrass.dto.ReservationDto;
import com.resergrass.dto.ReservationRequest;
import com.resergrass.dto.ReservationQuoteDto;
import com.resergrass.service.CourtPricingService;
import com.resergrass.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservationService;
    private final CourtPricingService pricingService;

    @GetMapping("/quote")
    public ReservationQuoteDto quote(
            @RequestParam Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        return pricingService.quote(courtId, date, startTime, endTime);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public List<ReservationDto> byCourtAndDate(
            @RequestParam Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reservationService.byCourtAndDate(courtId, date);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<ReservationDto> mine(@AuthenticationPrincipal User user) {
        return reservationService.mine(user);
    }

    @PostMapping
    public ReservationDto create(@Valid @RequestBody ReservationRequest request, @AuthenticationPrincipal User user) {
        return reservationService.create(request, user);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public ReservationDto updateStatus(@PathVariable Long id, @RequestParam ReservationStatus status) {
        return reservationService.updateStatus(id, status);
    }

    @PatchMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public ReservationDto updatePayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return reservationService.updatePayment(id, request);
    }

    @PatchMapping("/{id}/payment/confirm")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public ReservationDto confirmPayment(@PathVariable Long id, @RequestParam(required = false) String method) {
        return reservationService.confirmPayment(id, method);
    }

    @PatchMapping("/{id}/payment/reject")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public ReservationDto rejectPayment(@PathVariable Long id, @RequestParam String reason) {
        return reservationService.rejectPayment(id, reason);
    }

    @PatchMapping("/{id}/payment/local")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public ReservationDto markPayAtVenue(@PathVariable Long id) {
        return reservationService.markPayAtVenue(id);
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ReportDto report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reservationService.report(from, to);
    }
}
