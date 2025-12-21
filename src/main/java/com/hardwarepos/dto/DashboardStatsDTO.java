package com.hardwarepos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Double totalSales;
    private Double totalProfit;
    private Long orders;
    private Long lowStock;
    private Long totalProducts;
    private Integer storeCount;
    
    private List<ChartDataDTO> chartData;
    private List<StorePerformanceDTO> storePerformance; // For Owner Global View
    private List<TopProductDTO> topProducts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartDataDTO {
        private String name;
        private Double sales;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorePerformanceDTO {
        private String name;
        private Double sales;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDTO {
        private String name;
        private Long quantity;
    }
}
