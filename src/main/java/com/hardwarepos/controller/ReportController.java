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
            @RequestParam(required = false) Long storeId) {
        
        User user = securityService.getAuthenticatedUser();
        // Security check: Only OWNER or STORE_ADMIN should see full reports? 
        // Assuming Report Page is restricted to OWNER/STORE_ADMIN in frontend.
        // Backend check:
        // Owner can see any store (if storeId param provided), or all.
        // Link to ReportService: currently implementation takes storeId.
        
        Long effectiveStoreId = storeId;
        if (user.getRole() != User.Role.OWNER) {
            // Force user's store ID if not Owner
            effectiveStoreId = user.getStoreId();
        } 
        // If Owner and storeId is null -> Reports for ALL stores aggregated?
        // Service implementation handles find logic. 
        // If null passed to repo method, it might crash if using 'findAllByStoreId...'.
        // ReportService logic: 
        // if (storeId != null) findAllByStoreId... else findAll...
        // So Owner passing null gets Global Report. Correct.

        DailySalesReportDTO report = reportService.getDailySalesReport(date, effectiveStoreId);
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
