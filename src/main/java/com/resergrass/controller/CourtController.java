package com.resergrass.controller;

import com.resergrass.domain.enums.CourtStatus;
import com.resergrass.dto.*;
import com.resergrass.service.CourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courts")
@RequiredArgsConstructor
public class CourtController {
    private final CourtService courtService;

    @GetMapping
    public List<CourtDto> activeCourts() {
        return courtService.activeCourts();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CourtDto> all() {
        return courtService.all();
    }

    @GetMapping("/{id}")
    public CourtDto byId(@PathVariable Long id) {
        return courtService.byId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CourtDto create(@Valid @RequestBody CourtRequest request) {
        return courtService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourtDto update(@PathVariable Long id, @Valid @RequestBody CourtRequest request) {
        return courtService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CourtDto changeStatus(@PathVariable Long id, @RequestParam CourtStatus status) {
        return courtService.changeStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) {
        courtService.deactivate(id);
    }

    @PostMapping("/{id}/price-rules")
    @PreAuthorize("hasRole('ADMIN')")
    public CourtPriceRuleDto addPriceRule(@PathVariable Long id, @Valid @RequestBody CourtPriceRuleRequest request) {
        return courtService.addPriceRule(id, request);
    }

    @PostMapping("/{id}/promotions")
    @PreAuthorize("hasRole('ADMIN')")
    public CourtPromotionDto addPromotion(@PathVariable Long id, @Valid @RequestBody CourtPromotionRequest request) {
        return courtService.addPromotion(id, request);
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public CourtStatsDto stats(@PathVariable Long id) {
        return courtService.stats(id);
    }
}
