package com.hardwarepos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // Unique Voucher Code (e.g. V-12345)

    private Double amount;
    private Double currentBalance; // Allow partial usage if needed, or just one-time

    private String status; // ACTIVE, REDEEMED, EXPIRED

    private Long issuedForReturnId; // Link to DamageItem or Return Transaction if needed
    private LocalDateTime issuedDate;
    private LocalDateTime expiryDate;

    // Optional: Lock to customer phone? For now, open bearer voucher.

    @PrePersist
    protected void onCreate() {
        issuedDate = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (currentBalance == null) currentBalance = amount;
    }
}
