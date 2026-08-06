package com.resergrass.controller;

import com.resergrass.dto.CalendarSlotDto;
import com.resergrass.domain.entity.User;
import com.resergrass.dto.ScheduleDto;
import com.resergrass.dto.ScheduleRequest;
import com.resergrass.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleDto> byCourtAndDate(
            @RequestParam Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return scheduleService.byCourtAndDate(courtId, date);
    }

    @GetMapping("/calendar")
    public List<CalendarSlotDto> calendar(
            @RequestParam Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User user
    ) {
        return scheduleService.calendar(courtId, date, user);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ScheduleDto create(@Valid @RequestBody ScheduleRequest request) {
        return scheduleService.create(request);
    }
}
