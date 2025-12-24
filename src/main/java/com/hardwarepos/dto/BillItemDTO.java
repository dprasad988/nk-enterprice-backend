package com.hardwarepos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillItemDTO {
    private String productName;
    private Integer quantity;
    private Double price; // Selling Price (after discount)
    private Double cost;  // Cost Price
    private Double profit;
    private Double discountPercent;
    private Boolean isReturned;
    private String returnStatus; // e.g., "APPROVED", "PENDING"
}
