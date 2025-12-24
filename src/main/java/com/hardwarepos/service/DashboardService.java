package com.hardwarepos.service;

import com.hardwarepos.dto.DashboardStatsDTO;
import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.InventoryRepository;
import com.hardwarepos.repository.SaleRepository;
import com.hardwarepos.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    public DashboardStatsDTO getDashboardStats(User user, Long storeId) {
        boolean isOwner = user.getRole() == User.Role.OWNER;
        Long targetStoreId = isOwner && storeId != null ? storeId : user.getStoreId();
        
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        // 1. Basic Counts
        if (targetStoreId != null) {
            stats.setTotalSales(orElseZero(saleRepository.sumTotalSalesByStore(targetStoreId)));
            stats.setTotalProfit(orElseZero(saleRepository.sumTotalProfitByStore(targetStoreId)));
            stats.setOrders(saleRepository.countSalesByStore(targetStoreId));
            stats.setLowStock(inventoryRepository.countLowStockByStore(targetStoreId));
            stats.setTotalProducts(inventoryRepository.countByStoreId(targetStoreId));
        } else {
            stats.setTotalSales(orElseZero(saleRepository.sumTotalSalesGlobal()));
            if (isOwner) {
                stats.setTotalProfit(orElseZero(saleRepository.sumTotalProfitGlobal()));
            } else {
                stats.setTotalProfit(0.0);
            }
            stats.setOrders(saleRepository.countSalesGlobal());
            stats.setLowStock(inventoryRepository.countLowStockGlobal());
            stats.setTotalProducts(inventoryRepository.count());
        }
        
        stats.setStoreCount((int) storeRepository.count());

        // 2. Weekly Chart Data (Last 7 Days)
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        List<Sale> recentSales;
        if (targetStoreId != null) {
            recentSales = saleRepository.findByStoreIdAndSaleDateAfter(targetStoreId, sevenDaysAgo);
        } else {
            recentSales = saleRepository.findBySaleDateAfter(sevenDaysAgo);
        }

        Map<String, Double> salesByDate = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            salesByDate.put(d.format(DateTimeFormatter.ISO_DATE).substring(5), 0.0);
        }

        for (Sale s : recentSales) {
            String dateKey = s.getSaleDate().format(DateTimeFormatter.ISO_DATE).substring(5);
            salesByDate.put(dateKey, salesByDate.getOrDefault(dateKey, 0.0) + s.getTotalAmount());
        }

        stats.setChartData(salesByDate.entrySet().stream()
                .map(e -> new DashboardStatsDTO.ChartDataDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));


        // 3. Top Products (Top 5)
        List<Object[]> topProductsRaw;
        if (targetStoreId != null) {
             topProductsRaw = saleRepository.findTopSellingProductsByStore(targetStoreId, PageRequest.of(0, 5)).getContent();
        } else {
             topProductsRaw = saleRepository.findTopSellingProductsGlobal(PageRequest.of(0, 5)).getContent();
        }
        
        stats.setTopProducts(topProductsRaw.stream()
                .map(obj -> new DashboardStatsDTO.TopProductDTO((String) obj[0], (Long) obj[1]))
                .collect(Collectors.toList()));


        // 4. Store Performance (Owner Global Only)
        if (targetStoreId == null && isOwner) {
            List<DashboardStatsDTO.StorePerformanceDTO> perf = new ArrayList<>();
            storeRepository.findAll().forEach(store -> {
                Double total = orElseZero(saleRepository.sumTotalSalesByStore(store.getId()));
                if (total > 0) {
                    perf.add(new DashboardStatsDTO.StorePerformanceDTO(store.getName(), total));
                }
            });
            stats.setStorePerformance(perf);
        }

        return stats;
    }

    private Double orElseZero(Double val) {
        return val == null ? 0.0 : val;
    }
}
