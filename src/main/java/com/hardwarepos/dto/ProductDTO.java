package com.hardwarepos.dto;

import com.hardwarepos.entity.Inventory;

public class ProductDTO {
    private Long id; // Inventory ID (to allow editing specific store inventory)
    private Long productId; // Global Product ID
    private Long storeId;
    private String name;
    private String barcode;
    private String sku;
    private Double price;
    private Double costPrice;
    private Integer stock;
    private Double discount;
    private Integer alertLevel;
// private com.hardwarepos.entity.Category category; // Removed

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Integer getAlertLevel() { return alertLevel; }
    public void setAlertLevel(Integer alertLevel) { this.alertLevel = alertLevel; }
// Getters/Setters for category removed

    public static ProductDTO from(Inventory inv) {
        ProductDTO dto = new ProductDTO();
        dto.setId(inv.getProduct().getId()); 
        
        dto.setProductId(inv.getProduct().getId());
        dto.setStoreId(inv.getStoreId());
        dto.setName(inv.getProduct().getName());
        dto.setBarcode(inv.getProduct().getBarcode());
        dto.setSku(inv.getProduct().getSku());
        // dto.setCategory(inv.getProduct().getCategory()); // Removed
        
        dto.setPrice(inv.getPrice());
        dto.setCostPrice(inv.getCostPrice());
        dto.setStock(inv.getStock());
        dto.setDiscount(inv.getDiscount());
        dto.setAlertLevel(inv.getAlertLevel());
        return dto;
    }

    @Override
    public String toString() {
        return "ProductDTO{" +
                "id=" + id +
                ", productId=" + productId +
                ", storeId=" + storeId +
                ", name='" + name + '\'' +
                ", barcode='" + barcode + '\'' +
                ", sku='" + sku + '\'' +
                ", price=" + price +
                ", costPrice=" + costPrice +
                ", stock=" + stock +
                ", discount=" + discount +
                ", alertLevel=" + alertLevel +
//                    ", category=" + category +
                '}';
    }
}
