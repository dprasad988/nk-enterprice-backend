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
    private Boolean isReturned; // If this specific item was returned
    private String returnStatus; // e.g., "APPROVED", "PENDING"
    private String barcode;
    private Boolean isNew; // Added field
    private Integer addedQuantity; // How many added in this version (if > 0)

    public Boolean getIsReturned() {
        return isReturned;
    }
    public void setIsReturned(Boolean isReturned) {
        this.isReturned = isReturned;
    }

    public Boolean getIsNew() {
        return isNew;
    }
    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }
    
    public String getBarcode() {
        return barcode;
    }
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
