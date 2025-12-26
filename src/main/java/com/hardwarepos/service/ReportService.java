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
import java.util.Set; // Added import
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private DamageItemRepository damageItemRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private com.hardwarepos.repository.ProductRepository productRepository;
    @Autowired
    private com.hardwarepos.repository.SaleVersionRepository saleVersionRepository;

    public DailySalesReportDTO getDailySalesReport(LocalDate date, Long storeId, int page, int size, String search, String status) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // 1. Calculate Daily Summaries (Global for the day)
        Double dayRevenue;
        Double dayCost;
        
        if (storeId != null) {
            dayRevenue = saleRepository.sumTotalAmountByStoreAndDateRange(storeId, startOfDay, endOfDay);
            dayCost = saleRepository.sumTotalCostByStoreAndDateRange(storeId, startOfDay, endOfDay);
        } else {
            dayRevenue = saleRepository.sumTotalAmountByDateRange(startOfDay, endOfDay);
            dayCost = saleRepository.sumTotalCostByDateRange(startOfDay, endOfDay);
        }
        
        // Handle nulls and calculate profit
        dayRevenue = dayRevenue != null ? dayRevenue : 0.0;
        dayCost = dayCost != null ? dayCost : 0.0;
        double dayProfit = dayRevenue - dayCost;
        
        // Retrieve Paginated Data
        org.springframework.data.domain.Pageable pageRequest = org.springframework.data.domain.PageRequest.of(page, size);
        
        String searchPattern = null;
        Long searchId = null;
        
        if (search != null && !search.trim().isEmpty()) {
            searchPattern = "%" + search.toLowerCase() + "%";
            try {
                searchId = Long.parseLong(search.trim());
            } catch (NumberFormatException e) {
                // Not a number, so not an ID search
                searchId = null;
            }
        }
        
        org.springframework.data.domain.Page<Sale> salesPage = saleRepository.findDailySales(
            startOfDay, endOfDay, storeId, 
            searchPattern,
            searchId,
            status, 
            pageRequest
        );
        
        List<BillReportDTO> billReports = new ArrayList<>();
        for (Sale sale : salesPage.getContent()) {
            billReports.add(convertToBillReportDTO(sale));
        }
        
        // Pagination Metadata
        int totalPages = salesPage.getTotalPages();
        long totalElements = salesPage.getTotalElements();
        
        // Fetch accurate Daily Global Stats (not affected by search/status filter)
        Long totalBills;
        Long totalReturns;
        
        if (storeId != null) {
            totalBills = saleRepository.countTransactionsByStoreAndDateRange(storeId, startOfDay, endOfDay);
            totalReturns = saleRepository.countReturnsByStoreAndDateRange(storeId, startOfDay, endOfDay);
        } else {
            totalBills = saleRepository.countTransactionsByDateRange(startOfDay, endOfDay);
            totalReturns = saleRepository.countReturnsByDateRange(startOfDay, endOfDay);
        }
        
        return new DailySalesReportDTO(
            dayRevenue, 
            dayProfit, 
            totalBills != null ? totalBills.intValue() : 0, 
            totalReturns != null ? totalReturns.intValue() : 0, 
            page, 
            totalPages, 
            totalElements, 
            billReports
        );
    }

    private BillReportDTO convertToBillReportDTO(Sale sale) {
            BillReportDTO billDTO = new BillReportDTO();
            billDTO.setSaleId(sale.getId());
            billDTO.setBillNo(String.valueOf(sale.getId()));
            billDTO.setTime(sale.getSaleDate().toLocalTime().toString().substring(0, 5)); // HH:mm
            billDTO.setCashier(sale.getCashierName());
            billDTO.setTotalAmount(sale.getTotalAmount());
            billDTO.setPaymentMethod(sale.getPaymentMethod());
            
            // Get History (SaleVersions) 
            List<com.hardwarepos.entity.SaleVersion> versions = saleVersionRepository.findBySaleIdOrderByVersionAtDesc(sale.getId());
            boolean hasHistory = !versions.isEmpty();
            boolean isVoucherPayment = sale.getPaymentMethod() != null && sale.getPaymentMethod().contains("VOUCHER");
            
            billDTO.setIsExchange(hasHistory || isVoucherPayment); 
            billDTO.setIsVoucherUsed(isVoucherPayment);
            
            double billProfit = 0.0;
            List<BillItemDTO> itemDTOs = new ArrayList<>();
            
            List<DamageItem> returns = damageItemRepository.findByOriginalSaleId(sale.getId());
            boolean hasReturns = !returns.isEmpty();
            billDTO.setHasReturns(hasReturns);
            
            String voucherCode = returns.stream()
                .filter(d -> d.getStatus().equals("VOUCHER_ISSUED") && d.getVoucherCode() != null)
                .map(DamageItem::getVoucherCode)
                .findFirst().orElse(null);
            
            billDTO.setVoucherCode(voucherCode);
            if (voucherCode != null) {
                voucherRepository.findByCode(voucherCode).ifPresent(v -> {
                    billDTO.setVoucherStatus(v.getStatus()); 
                });
            }

            java.util.Map<String, Integer> previousQuantities = new java.util.HashMap<>();
            if (!versions.isEmpty()) {
                com.hardwarepos.entity.SaleVersion previousState = versions.get(0); 
                if (previousState.getItems() != null) {
                    for (com.hardwarepos.entity.SaleVersionItem pItem : previousState.getItems()) {
                         String barcode = (pItem.getBarcode() != null && !pItem.getBarcode().isEmpty()) ? " [" + pItem.getBarcode() + "]" : "";
                         String sig = pItem.getProductName() + barcode;
                         previousQuantities.merge(sig, pItem.getQuantity(), Integer::sum);
                    }
                }
            }

            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    BillItemDTO itemDTO = new BillItemDTO();
                    itemDTO.setProductName(item.getProductName());
                    itemDTO.setQuantity(item.getQuantity());
                    
                    if (item.getProductId() != null) {
                        productRepository.findById(item.getProductId())
                            .ifPresent(p -> itemDTO.setBarcode(p.getBarcode()));
                    }
                    
                    double finalPrice = item.getPrice();
                    if (item.getDiscount() != null && item.getDiscount() > 0) {
                        finalPrice = item.getPrice() * (1 - item.getDiscount() / 100.0);
                        itemDTO.setDiscountPercent(item.getDiscount());
                    } else {
                        itemDTO.setDiscountPercent(0.0);
                    }
                    
                    itemDTO.setPrice(finalPrice);
                    itemDTO.setCost(item.getCostPrice() != null ? item.getCostPrice() : 0.0);
                    
                    double itemProfit = (finalPrice - itemDTO.getCost()) * item.getQuantity();
                    itemDTO.setProfit(itemProfit);
                    itemDTO.setIsReturned(false); 
                    
                    String currentSig = item.getProductName() + (itemDTO.getBarcode() != null ? " [" + itemDTO.getBarcode() + "]" : "");
                    
                    if (!versions.isEmpty()) {
                        int prevQty = previousQuantities.getOrDefault(currentSig, 0);
                        int currentQty = item.getQuantity();
                        
                        if (prevQty == 0) {
                            itemDTO.setIsNew(true);
                            itemDTO.setAddedQuantity(currentQty);
                        } else if (currentQty > prevQty) {
                            itemDTO.setIsNew(true);
                            itemDTO.setAddedQuantity(currentQty - prevQty);
                        } else {
                            itemDTO.setIsNew(false);
                            itemDTO.setAddedQuantity(0);
                        }
                    } else {
                        itemDTO.setIsNew(false);
                        itemDTO.setAddedQuantity(0);
                    }

                    itemDTOs.add(itemDTO);
                    billProfit += itemProfit;
                }
            }

            for (DamageItem returnedItem : returns) {
                boolean alreadyInList = itemDTOs.stream().anyMatch(dto -> dto.getProductName().equals(returnedItem.getProductName()) && dto.getIsReturned());
                if (!alreadyInList) {
                    BillItemDTO returnDTO = new BillItemDTO();
                    returnDTO.setProductName(returnedItem.getProductName() + " (Returned)"); 
                    returnDTO.setQuantity(returnedItem.getQuantity());
                    returnDTO.setPrice(returnedItem.getRefundAmount() / returnedItem.getQuantity()); 
                    returnDTO.setCost(0.0); 
                    returnDTO.setDiscountPercent(0.0);
                    returnDTO.setProfit(0.0); 
                    returnDTO.setIsReturned(true);
                    
                    itemDTOs.add(returnDTO);
                }
            }

            if (sale.getDiscount() != null && sale.getDiscount() > 0) {
                 double r = sale.getDiscount() / 100.0;
                 if (r < 1.0) { 
                     double impliedSubtotal = sale.getTotalAmount() / (1.0 - r);
                     double discountVal = impliedSubtotal - sale.getTotalAmount();
                     billProfit -= discountVal;
                 }
            }
            
            billDTO.setProfit(billProfit);
            billDTO.setItems(itemDTOs);
            return billDTO;
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
