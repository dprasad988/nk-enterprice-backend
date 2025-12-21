package com.hardwarepos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class DamageItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long originalSaleId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double refundAmount; // The value given back as voucher

    private LocalDateTime returnDate;
    private String reason; // "Damaged", etc.
    private String returnedBy; // Cashier username

    private String status; // PENDING, APPROVED, REJECTED, VOUCHER_ISSUED
    private LocalDateTime approvalDate;
    private String approvedBy;
    private String voucherCode;
    private Long storeId;

    @PrePersist
    protected void onCreate() {
        returnDate = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
