package com.hardwarepos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitReportDTO {
    private Double todayProfit;
    private Double weeklyProfit;
    private Double monthlyProfit;
    private Double yearlyProfit;
    
    private List<ChartDataPoint> chartData;

    @Data
    @AllArgsConstructor
    public static class ChartDataPoint {
        private String name; // Label (e.g., "Mon", "Jan")
        private String fullDate; // YYYY-MM-DD or similar for filtering if needed
        private Double profit;
    }
}
