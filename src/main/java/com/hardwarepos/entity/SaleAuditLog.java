package com.hardwarepos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // "SALE", "EXCHANGE", "VOID" (if added)
    
    private Long saleId;
    
    private Long storeId;
    
    // We store billNo specifically if it differs or for ease
    private String billNo; 
    
    private String actionBy; // Username (Cashier)
    
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String details;
}
