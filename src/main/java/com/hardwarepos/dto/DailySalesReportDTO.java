package com.hardwarepos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesReportDTO {
    private Double totalSales;
    private Double totalProfit;
    private Integer totalBills;
    private Integer totalReturns; // Count of bills with returns
    private List<BillReportDTO> bills;
}
