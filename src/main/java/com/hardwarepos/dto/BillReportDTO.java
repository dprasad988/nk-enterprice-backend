package com.hardwarepos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillReportDTO {
    private Long saleId;
    private String billNo;
    private String time; // Formatted Time
    private String cashier;
    private Double totalAmount;
    private Double profit;
    private Boolean isExchange;
    private String paymentMethod;
    private List<BillItemDTO> items;
    
    // Return & Voucher Info
    private Boolean hasReturns;
    private String voucherCode; // If issued
    private String voucherStatus; // REDEEMED / ISSUED
    private Boolean isVoucherUsed; // If this bill USED a voucher
}
