package com.hardwarepos.repository;

import com.hardwarepos.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    // Basic reporting hooks
    List<Sale> findAllByOrderBySaleDateDesc();
    List<Sale> findAllByStoreIdOrderBySaleDateDesc(Long storeId);
    List<Sale> findByStoreId(Long storeId);

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
}
