package com.hardwarepos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Sale sale; // The parent active sale

    private LocalDateTime versionAt; // When this version was created (snapshot time)
    private String versionReason; // "ORIGINAL", "PRE-UPDATE", "PRE-EXCHANGE"

    // Snapshot fields
    private Double totalAmount;
    private String paymentMethod;
    private String cashierName;
    private Double discount;

    @OneToMany(mappedBy = "saleVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleVersionItem> items;
}
