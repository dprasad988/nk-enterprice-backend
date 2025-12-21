package com.hardwarepos.scheduler;

import com.hardwarepos.service.EmailService;
import com.hardwarepos.service.PdfReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReportScheduler {

    @Autowired
    private PdfReportService pdfReportService;

    @Autowired
    private EmailService emailService;
    
    // Configurable recipient from properties or hardcoded
    @Value("${report.notification.email:owner@example.com}")
    private String ownerEmail;

    // Run at 12:00 AM every day
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleDailyReport() {
        System.out.println("Generating Daily Profit Report...");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1); // Report for the finished day
            byte[] pdfContent = pdfReportService.generateDailyProfitReport(yesterday);
            
            String subject = "Daily Profit Report - " + yesterday;
            String body = "Please find attached the daily profit report for " + yesterday + ".";
            String filename = "DailyReport_" + yesterday + ".pdf";
            
            emailService.sendEmailWithAttachment(ownerEmail, subject, body, pdfContent, filename);
            
        } catch (Exception e) {
            System.err.println("Error in Daily Report Scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
