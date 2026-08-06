package com.resergrass.controller;

import com.resergrass.dto.OperationsDashboardDto;
import com.resergrass.service.OperationsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class OperationsDashboardController {
    private final OperationsDashboardService dashboardService;

    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public OperationsDashboardDto operations() {
        return dashboardService.today();
    }
}
