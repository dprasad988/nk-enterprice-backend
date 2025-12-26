package com.hardwarepos.controller;

import com.hardwarepos.dto.DailySalesReportDTO;
import com.hardwarepos.entity.User;
import com.hardwarepos.service.ReportService;
import com.hardwarepos.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")

public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private SecurityService securityService;

    @GetMapping("/daily-sales")
    public ResponseEntity<DailySalesReportDTO> getDailySalesReport(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        
        User user = securityService.getAuthenticatedUser();
        Long effectiveStoreId = storeId;
        if (user.getRole() != User.Role.OWNER) {
            effectiveStoreId = user.getStoreId();
        } 

        DailySalesReportDTO report = reportService.getDailySalesReport(date, effectiveStoreId, page, size, search, status);
        return ResponseEntity.ok(report);
    }
    @GetMapping("/profit-summary")
    public ResponseEntity<com.hardwarepos.dto.ProfitReportDTO> getProfitSummary(
            @RequestParam(required = false, defaultValue = "last7days") String chartRange,
            @RequestParam(required = false) Long storeId) {
        
        User user = securityService.getAuthenticatedUser();
        Long effectiveStoreId = storeId;
        if (user.getRole() != User.Role.OWNER) {
            effectiveStoreId = user.getStoreId();
        }

        com.hardwarepos.dto.ProfitReportDTO report = reportService.getProfitSummary(effectiveStoreId, chartRange);
        return ResponseEntity.ok(report);
    }
}
