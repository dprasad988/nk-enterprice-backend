package com.hardwarepos.controller;

import com.hardwarepos.dto.DashboardStatsDTO;
import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.InventoryRepository;
import com.hardwarepos.repository.SaleRepository;
import com.hardwarepos.repository.StoreRepository;
import com.hardwarepos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/stats")
    public DashboardStatsDTO getDashboardStats(@RequestParam(required = false) Long storeId) {
        User user = getAuthenticatedUser();
        boolean isOwner = user.getRole() == User.Role.OWNER;
        Long targetStoreId = isOwner && storeId != null ? storeId : user.getStoreId();
        
        // Handle "ALL" case for Owner (storeId passed might be specific, but if null implies global?)
        // Frontend sends storeId only if specific, or maybe 'ALL' handling happens there. 
        // If targetStoreId is null here, it means Global View.
        
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
                stats.setTotalProfit(0.0); // Hide global profit from non-owners if they ever reach here
            }
            stats.setOrders(saleRepository.countSalesGlobal());
            stats.setLowStock(inventoryRepository.countLowStockGlobal());
            stats.setTotalProducts(inventoryRepository.count()); // count() is total inventory entries
        }
        
        stats.setStoreCount((int) storeRepository.count());

        // 2. Weekly Chart Data (Last 7 Days)
        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay(); // 7 days including today
        List<Sale> recentSales;
        if (targetStoreId != null) {
            recentSales = saleRepository.findByStoreIdAndSaleDateAfter(targetStoreId, sevenDaysAgo);
        } else {
            // Only fetch recent sales for global if strictly needed query-wise
            // For Global Chart, better to just let Java Aggregate?
            recentSales = saleRepository.findBySaleDateAfter(sevenDaysAgo);
        }

        Map<String, Double> salesByDate = new TreeMap<>();
        // Initialize last 7 days with 0
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            salesByDate.put(d.format(DateTimeFormatter.ISO_DATE).substring(5), 0.0); // "MM-DD"
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
            // Aggregate all sales by Store... existing repo method?
            // "SELECT s.storeId, SUM(s.totalAmount) ..."
            // Or just iterate stores and ask for sum? N+1 for stores (usually few stores < 100)
            // Better: use one query. 
            // Query: "SELECT s.storeId, SUM(s.totalAmount) FROM Sale s GROUP BY s.storeId"
            // Let's do a quick inline aggregation here via Java on recentSales? No, performance needs ALL time sales.
            // Let's add one more query to SaleRepository or just stick to "recent sales" for performance?
            // Dashboard says "Store Performance (Sales)" usually implies total?
            // Existing frontend code did: `sales.forEach...` on ALL sales.
            // Let's add `sumTotalSalesGroupByStore` to Repository?
            // Or just leave empty for now?
            // For now, let's skip or simplify. 
            // Let's leave it empty to see if user notices, OR better, add the query.
            // I'll add the query to SaleRepository in a separate step or just use Java loop if stores are few.
            // Assuming simplified: List all stores, and for each call sumTotalSalesByStore. (N+1 for Stores, expected < 10 stores). acceptable.
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
