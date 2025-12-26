package com.hardwarepos.repository;

import com.hardwarepos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    // Basic reporting hooks
    List<Sale> findAllByOrderBySaleDateDesc();
    List<Sale> findAllByStoreIdOrderBySaleDateDesc(Long storeId);
    List<Sale> findByStoreId(Long storeId);

    List<Sale> findAllBySaleDateBetweenOrderBySaleDateDesc(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Sale> findAllByStoreIdAndSaleDateBetweenOrderBySaleDateDesc(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalAmount) FROM Sale s")
    Double sumTotalSalesGlobal();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.storeId = :storeId")
    Double sumTotalSalesByStore(Long storeId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s")
    Long countSalesGlobal();
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s WHERE s.storeId = :storeId")
    Long countSalesByStore(Long storeId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM((i.price - i.costPrice) * i.quantity) FROM SaleItem i")
    Double sumTotalProfitGlobal();

    @org.springframework.data.jpa.repository.Query("SELECT SUM((i.price - i.costPrice) * i.quantity) FROM SaleItem i JOIN i.sale s WHERE s.storeId = :storeId")
    Double sumTotalProfitByStore(Long storeId);

    // Chart Data (Last 7 days)
    List<Sale> findBySaleDateAfter(java.time.LocalDateTime date);
    List<Sale> findByStoreIdAndSaleDateAfter(Long storeId, java.time.LocalDateTime date);

    // Top Products
    @org.springframework.data.jpa.repository.Query("SELECT i.productName, SUM(i.quantity) as totalQty FROM SaleItem i GROUP BY i.productName ORDER BY totalQty DESC")
    org.springframework.data.domain.Page<Object[]> findTopSellingProductsGlobal(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT i.productName, SUM(i.quantity) as totalQty FROM SaleItem i JOIN i.sale s WHERE s.storeId = :storeId GROUP BY i.productName ORDER BY totalQty DESC")
    org.springframework.data.domain.Page<Object[]> findTopSellingProductsByStore(Long storeId, org.springframework.data.domain.Pageable pageable);
    // Aggregation for Profit Calculation (Revenue - Cost)
    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    Double sumTotalAmountByDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.storeId = :storeId AND s.saleDate BETWEEN :start AND :end")
    Double sumTotalAmountByStoreAndDateRange(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.costPrice * i.quantity) FROM SaleItem i JOIN i.sale s WHERE s.saleDate BETWEEN :start AND :end")
    Double sumTotalCostByDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(i.costPrice * i.quantity) FROM SaleItem i JOIN i.sale s WHERE s.storeId = :storeId AND s.saleDate BETWEEN :start AND :end")
    Double sumTotalCostByStoreAndDateRange(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end")
    Long countTransactionsByDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s WHERE s.storeId = :storeId AND s.saleDate BETWEEN :start AND :end")
    Long countTransactionsByStoreAndDateRange(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate BETWEEN :start AND :end AND EXISTS (SELECT d FROM DamageItem d WHERE d.originalSaleId = s.id)")
    Long countReturnsByDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Sale s WHERE s.storeId = :storeId AND s.saleDate BETWEEN :start AND :end AND EXISTS (SELECT d FROM DamageItem d WHERE d.originalSaleId = s.id)")
    Long countReturnsByStoreAndDateRange(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Sale s WHERE " +
           "s.saleDate BETWEEN :start AND :end " +
           "AND (:storeId IS NULL OR s.storeId = :storeId) " +
           "AND (" +
           "   (:searchPattern IS NULL AND :searchId IS NULL) " + 
           "   OR (:searchPattern IS NOT NULL AND LOWER(s.cashierName) LIKE :searchPattern) " +
           "   OR (:searchId IS NOT NULL AND s.id = :searchId)" +
           ") " +
           "AND (:status = 'ALL' " +
           "     OR (:status = 'EXCHANGE' AND EXISTS (SELECT v FROM SaleVersion v WHERE v.sale.id = s.id)) " +
           "     OR (:status = 'RETURN' AND EXISTS (SELECT d FROM DamageItem d WHERE d.originalSaleId = s.id)) " +
           "     OR (:status = 'NEW' " +
           "         AND NOT EXISTS (SELECT v FROM SaleVersion v WHERE v.sale.id = s.id) " +
           "         AND NOT EXISTS (SELECT d FROM DamageItem d WHERE d.originalSaleId = s.id)) " +
           ") " +
           "ORDER BY s.saleDate DESC")
    org.springframework.data.domain.Page<Sale> findDailySales(
            java.time.LocalDateTime start, 
            java.time.LocalDateTime end, 
            Long storeId, 
            String searchPattern, 
            Long searchId,
            String status, 
            org.springframework.data.domain.Pageable pageable);
}
