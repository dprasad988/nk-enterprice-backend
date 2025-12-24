package com.hardwarepos.service;

import com.hardwarepos.dto.BillItemDTO;
import com.hardwarepos.dto.BillReportDTO;
import com.hardwarepos.dto.DailySalesReportDTO;
import com.hardwarepos.entity.DamageItem;
import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.SaleItem;
import com.hardwarepos.entity.Voucher;
import com.hardwarepos.repository.DamageItemRepository;
import com.hardwarepos.repository.SaleRepository;
import com.hardwarepos.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private DamageItemRepository damageItemRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    public DailySalesReportDTO getDailySalesReport(LocalDate date, Long storeId) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Sale> sales;
        if (storeId != null) {
            sales = saleRepository.findAllByStoreIdAndSaleDateBetweenOrderBySaleDateDesc(storeId, startOfDay, endOfDay);
            // Hint: ensure repository method exists or use generic find attributes
        } else {
             // For Owner view of ALL stores? Or strictly one store? 
             // Logic in SaleService suggests Owner can see all.
             sales = saleRepository.findAllBySaleDateBetweenOrderBySaleDateDesc(startOfDay, endOfDay);
        }

        double totalSales = 0.0;
        double totalProfit = 0.0;
        int totalBills = sales.size();
        int totalReturns = 0;

        List<BillReportDTO> billReports = new ArrayList<>();

        for (Sale sale : sales) {
            BillReportDTO billDTO = new BillReportDTO();
            billDTO.setSaleId(sale.getId());
            billDTO.setBillNo(String.valueOf(sale.getId()));
            billDTO.setTime(sale.getSaleDate().toLocalTime().toString().substring(0, 5)); // HH:mm
            billDTO.setCashier(sale.getCashierName());
            billDTO.setTotalAmount(sale.getTotalAmount());
            billDTO.setPaymentMethod(sale.getPaymentMethod());
            
            // Determine Exchange
            // Logic: if audit log says "Exchanged" or if PaymentMethod has "PARTIAL VOUCHER"?
            // Or simpler: check if payment method contains "VOUCHER" logic?
            // Actually user asked "how it make sence for profit". So explicit flag is good.
            // For now, assume based on pm string or if updated.
            billDTO.setIsExchange(sale.getPaymentMethod().contains("VOUCHER")); 
            billDTO.setIsVoucherUsed(sale.getPaymentMethod().contains("VOUCHER"));

            // Calculate Profit & Items
            double billProfit = 0.0;
            List<BillItemDTO> itemDTOs = new ArrayList<>();
            
            // Check for Returns (DamageItems linked to THIS sale)
            List<DamageItem> returns = damageItemRepository.findByOriginalSaleId(sale.getId());
            boolean hasReturns = !returns.isEmpty();
            if (hasReturns) totalReturns++;
            billDTO.setHasReturns(hasReturns);

            // Check Voucher Link (from returns)
            // If any return has status VOUCHER_ISSUED, find that voucher
            // Note: Multiple items might return -> One voucher? Or multiple?
            // Usually grouped. 
            // Let's find any unique voucher code in the DamageItems
            String voucherCode = returns.stream()
                .filter(d -> d.getStatus().equals("VOUCHER_ISSUED") && d.getVoucherCode() != null)
                .map(DamageItem::getVoucherCode)
                .findFirst().orElse(null);
            
            billDTO.setVoucherCode(voucherCode);
            if (voucherCode != null) {
                voucherRepository.findByCode(voucherCode).ifPresent(v -> {
                    billDTO.setVoucherStatus(v.getStatus()); // ISSUED or REDEEMED
                });
            }

            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    BillItemDTO itemDTO = new BillItemDTO();
                    itemDTO.setProductName(item.getProductName());
                    itemDTO.setQuantity(item.getQuantity());
                    
                    double finalPrice = item.getPrice();
                    // Apply item discount if any? Logic in SaleService creates subtotal immediately.
                    // But we store 'discount' on item (percentage).
                    if (item.getDiscount() != null && item.getDiscount() > 0) {
                        finalPrice = item.getPrice() * (1 - item.getDiscount() / 100.0);
                        itemDTO.setDiscountPercent(item.getDiscount());
                    } else {
                        itemDTO.setDiscountPercent(0.0);
                    }
                    
                    itemDTO.setPrice(finalPrice);
                    itemDTO.setCost(item.getCostPrice() != null ? item.getCostPrice() : 0.0);
                    
                    // Profit = (Selling - Cost) * Qty
                    // Wait, must also account for Bill-level discount?
                    // Usually bill discount reduces total.
                    // Let's calculate Gross Item Profit first.
                    double itemProfit = (finalPrice - itemDTO.getCost()) * item.getQuantity();
                    itemDTO.setProfit(itemProfit);


                    // Check if this specific item was returned?
                    // We have `returns` list. Matches ProductID.
                    // If returned, we should NOT count it in profit? 
                    // Or display it as returned?
                    // User asked "if a bill had return... how it make sence for profit"
                    // Ideally: Profit = Revenue - Cost.
                    // If returned & voucher issued: Revenue is effectively retained (as voucher liability), but goods are back (bad stock).
                    // If "Damaged", the cost is LOST (Loss).
                    // Logic:
                    // 1. Sale happened. +Profit.
                    // 2. Return (Damaged). -Revenue (Voucher refund) AND -Cost (Item destroyed/loss).
                    // Actually, if damaged, we lost the item cost.
                    // And if we gave a voucher, we effectively gave back the revenue.
                    // So Profit = 0 (Revenue - Refund) - Cost (Product Cost). = -Cost.
                    // BUT, `DailySalesReport` is for a SPECIFIC DATE.
                    // If the return happened LATER (another day), does it affect TODAY's report?
                    // User said "track full details day by day".
                    // If I look at strict "Sales on Day X", the profit WAS made on Day X.
                    // The return is a separate event that might happen on Day Y.
                    // However, showing the *current* status of that bill (even if returned later) is helpful context.
                    // BUT strictly changing the "Total Profit" of Day X because of a return on Day Y is controversial accounting.
                    // USUALLY: Sales Report for Day X shows the sales AS THEY HAPPENED.
                    // Returns are separate negative entries on the day they happen.
                    
                    // COMPROMISE:
                    // 1. Show the ORIGINAL profit of the sale.
                    // 2. Show "Has Return" flag.
                    // 3. DO NOT deduct from "Total Profit" of Day X if the return happened later.
                    // 4. IF the return happened TODAY, maybe deduct?
                    // Complexity: Logic for "return date".
                    // Let's stick to: "Profit from Deal".
                    // Report shows the sale's financial result.
                    // If I return it, the sale is effectively cancelled/reversed.
                    // I will SUBTRACT returned items from the bill's profit for display purposes to show "Realized Profit".
                    // Note: If damaged, we lose the cost.
                    
                    // Simple Logic for now:
                    // Profit = Sum(ItemProfit).
                    // If Bill Level Discount > 0:
                    // Net Profit = Sum(ItemProfit) - (Total * Discount / 100).
                    
                    // Mark item as returned if matched in `returns`
                    boolean isReturned = returns.stream().anyMatch(r -> r.getProductId().equals(item.getProductId()));
                    itemDTO.setIsReturned(isReturned);
                    
                    itemDTOs.add(itemDTO);
                    billProfit += itemProfit;
                }
            }

            // Deduct Bill Global Discount
            if (sale.getDiscount() != null && sale.getDiscount() > 0) {
                 double discountAmount = sale.getTotalAmount() * (sale.getDiscount() / 100.0);
                 // Wait, totalAmount is AFTER discount in basic logic?
                 // Sale.totalAmount usually stores final.
                 // Let's check `SaleService` create.
                 // `total = subtotal - (subtotal * (discount / 100));`
                 // So `totalAmount` is Net.
                 // My `itemProfit` calculation used `finalPrice` (item level).
                 // It did NOT account for Global Discount.
                 // So I must subtract the Global Discount value from the aggregated item profit.
                 // Global Discount Value = Sum(ItemSubtotals) * (GlobalRate/100).
                 // Approximation: (GrossProfit) - (Global Discount Amount).
                 // Wait, Global Discount reduces Revenue. Cost stays same.
                 // So Profit reduces by exactly the Discount Amount.
                 // We need to calculate the Discount Amount.
                 // `subtotal` isn't stored in Sale, only Total.
                 // `Total = Subtotal * (1 - rate)`.
                 // `Subtotal = Total / (1 - rate)`.
                 // `DiscountAmt = Subtotal - Total`.
                 
                 double r = sale.getDiscount() / 100.0;
                 if (r < 1.0) { // Safety
                     double impliedSubtotal = sale.getTotalAmount() / (1.0 - r);
                     double discountVal = impliedSubtotal - sale.getTotalAmount();
                     billProfit -= discountVal;
                 }
            }
            
            billDTO.setProfit(billProfit);
            billDTO.setItems(itemDTOs);
            billReports.add(billDTO);

            totalSales += sale.getTotalAmount();
            totalProfit += billProfit;
        }

        return new DailySalesReportDTO(totalSales, totalProfit, totalBills, totalReturns, billReports);
    }
    public com.hardwarepos.dto.ProfitReportDTO getProfitSummary(Long storeId, String chartRange) {
        LocalDate today = LocalDate.now();
        
        // 1. Calculate Card Stats
        Double todayProfit = calculateProfitForRange(today.atStartOfDay(), today.atTime(LocalTime.MAX), storeId);
        
        Double weeklyProfit = calculateProfitForRange(today.minusDays(6).atStartOfDay(), today.atTime(LocalTime.MAX), storeId);
        
        Double monthlyProfit = calculateProfitForRange(today.withDayOfMonth(1).atStartOfDay(), today.atTime(LocalTime.MAX), storeId);
        
        Double yearlyProfit = calculateProfitForRange(today.withDayOfYear(1).atStartOfDay(), today.atTime(LocalTime.MAX), storeId);

        // 2. Calculate Chart Data
        List<com.hardwarepos.dto.ProfitReportDTO.ChartDataPoint> chartData = new ArrayList<>();
        
        if ("last7days".equals(chartRange)) {
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                Double profit = calculateProfitForRange(d.atStartOfDay(), d.atTime(LocalTime.MAX), storeId);
                chartData.add(new com.hardwarepos.dto.ProfitReportDTO.ChartDataPoint(
                    d.toString().substring(5), // MM-DD
                    d.toString(),
                    profit
                ));
            }
        } else if ("last4weeks".equals(chartRange)) {
            // Last 5 Weeks
            for (int i = 4; i >= 0; i--) {
                LocalDate endOfWeek = today.minusWeeks(i); 
                // Define week as 7 days ending on that day? Or Standard Weeks?
                // Dashboard uses "Last 7 Days" logic blocks.
                // Let's use 7-day blocks ending on today (rolling weeks).
                LocalDate startOfWeek = endOfWeek.minusDays(6);
                
                Double profit = calculateProfitForRange(startOfWeek.atStartOfDay(), endOfWeek.atTime(LocalTime.MAX), storeId);
                chartData.add(new com.hardwarepos.dto.ProfitReportDTO.ChartDataPoint(
                    "Week " + endOfWeek.toString().substring(5),
                    endOfWeek.toString(), 
                    profit
                ));
            }
        } else if ("last12months".equals(chartRange)) {
            for (int i = 11; i >= 0; i--) {
                LocalDate d = today.minusMonths(i);
                LocalDate startOfMonth = d.withDayOfMonth(1);
                LocalDate endOfMonth = d.withDayOfMonth(d.lengthOfMonth());
                
                Double profit = calculateProfitForRange(startOfMonth.atStartOfDay(), endOfMonth.atTime(LocalTime.MAX), storeId);
                chartData.add(new com.hardwarepos.dto.ProfitReportDTO.ChartDataPoint(
                    d.toString().substring(0, 7), // YYYY-MM
                    d.toString().substring(0, 7),
                    profit
                ));
            }
        } else if ("last5years".equals(chartRange)) {
            for (int i = 4; i >= 0; i--) {
                LocalDate d = today.minusYears(i);
                LocalDate startOfYear = d.withDayOfYear(1);
                LocalDate endOfYear = d.withDayOfYear(d.lengthOfYear());
                
                Double profit = calculateProfitForRange(startOfYear.atStartOfDay(), endOfYear.atTime(LocalTime.MAX), storeId);
                chartData.add(new com.hardwarepos.dto.ProfitReportDTO.ChartDataPoint(
                    String.valueOf(d.getYear()),
                    String.valueOf(d.getYear()),
                    profit
                ));
            }
        }

        return new com.hardwarepos.dto.ProfitReportDTO(todayProfit, weeklyProfit, monthlyProfit, yearlyProfit, chartData);
    }

    private Double calculateProfitForRange(LocalDateTime start, LocalDateTime end, Long storeId) {
        Double revenue;
        Double cost;
        
        if (storeId != null) {
            revenue = saleRepository.sumTotalAmountByStoreAndDateRange(storeId, start, end);
            cost = saleRepository.sumTotalCostByStoreAndDateRange(storeId, start, end);
        } else {
            revenue = saleRepository.sumTotalAmountByDateRange(start, end);
            cost = saleRepository.sumTotalCostByDateRange(start, end);
        }
        
        revenue = revenue != null ? revenue : 0.0;
        cost = cost != null ? cost : 0.0;
        
        return revenue - cost;
    }
}
