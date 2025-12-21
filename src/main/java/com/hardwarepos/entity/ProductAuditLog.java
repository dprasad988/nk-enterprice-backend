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
public class ProductAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // "ADD", "EDIT", "DELETE"
    
    private Long productId;
    
    private Long storeId;

    private String productName;
    
    private String actionBy; // Username
    
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String details;
}
