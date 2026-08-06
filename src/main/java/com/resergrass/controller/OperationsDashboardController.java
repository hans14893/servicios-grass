package com.resergrass.controller;

import com.resergrass.domain.entity.User;
import com.resergrass.dto.ClientDashboardDto;
import com.resergrass.dto.OperationsDashboardDto;
import com.resergrass.service.ClientDashboardService;
import com.resergrass.service.OperationsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class OperationsDashboardController {
    private final OperationsDashboardService dashboardService;
    private final ClientDashboardService clientDashboardService;

    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('PERSONAL','ADMIN')")
    public OperationsDashboardDto operations() {
        return dashboardService.today();
    }

    @GetMapping("/client")
    @PreAuthorize("hasRole('CLIENTE')")
    public ClientDashboardDto client(@AuthenticationPrincipal User user) {
        return clientDashboardService.dashboard(user.getId());
    }
}
