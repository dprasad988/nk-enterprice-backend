package com.hardwarepos.controller;

import com.hardwarepos.dto.DashboardStatsDTO;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.UserRepository;
import com.hardwarepos.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private com.hardwarepos.service.SecurityService securityService;

    @GetMapping("/stats")
    public DashboardStatsDTO getDashboardStats(@RequestParam(required = false) Long storeId) {
        return dashboardService.getDashboardStats(securityService.getAuthenticatedUser(), storeId);
    }
}
