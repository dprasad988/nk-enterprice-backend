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
    


    public Long getSaleId() {
        return saleId;
    }
    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }
    public String getBillNo() {
        return billNo;
    }
    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public String getCashier() {
        return cashier;
    }
    public void setCashier(String cashier) {
        this.cashier = cashier;
    }
    public Double getTotalAmount() {
        return totalAmount;
    }
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    public String getPaymentMethod() {
        return paymentMethod;
    }
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    public Double getProfit() {
        return profit;
    }
    public void setProfit(Double profit) {
        this.profit = profit;
    }
    public Boolean getIsExchange() {
        return isExchange;
    }
    public void setIsExchange(Boolean isExchange) {
        this.isExchange = isExchange;
    }
    public Boolean getHasReturns() {
        return hasReturns;
    }
    public void setHasReturns(Boolean hasReturns) {
        this.hasReturns = hasReturns;
    }
    public java.util.List<BillItemDTO> getItems() {
        return items;
    }
    public void setItems(java.util.List<BillItemDTO> items) {
        this.items = items;
    }
    public Boolean getIsVoucherUsed() {
        return isVoucherUsed;
    }
    public void setIsVoucherUsed(Boolean isVoucherUsed) {
        this.isVoucherUsed = isVoucherUsed;
    }
    public String getVoucherCode() {
        return voucherCode;
    }
    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
    public String getVoucherStatus() {
        return voucherStatus;
    }
    public void setVoucherStatus(String voucherStatus) {
        this.voucherStatus = voucherStatus;
    }
    

}
