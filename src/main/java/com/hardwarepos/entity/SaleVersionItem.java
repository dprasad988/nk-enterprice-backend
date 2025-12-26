package com.hardwarepos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleVersionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_version_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SaleVersion saleVersion;

    private Long productId; // Store ID reference, even if product deleted later? Or just keep metadata.
    private String productName;
    private String barcode;
    
    private Integer quantity;
    private Double price;
    private Double costPrice; 
    private Double discount;
    private Double subtotal;
}
