package com.hardwarepos.repository;

import com.hardwarepos.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.storeId = :storeId")
    List<Inventory> findAllByStoreId(Long storeId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.storeId = :storeId")
    org.springframework.data.domain.Page<Inventory> findAllByStoreId(Long storeId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.storeId = :storeId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR p.barcode LIKE CONCAT('%', :query, '%'))")
    org.springframework.data.domain.Page<Inventory> searchByStore(@org.springframework.data.repository.query.Param("storeId") Long storeId, @org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);

    Optional<Inventory> findByStoreIdAndProductId(Long storeId, Long productId);
    Optional<Inventory> findByStoreIdAndProductBarcode(Long storeId, String barcode);

    Long countByStoreId(Long storeId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM Inventory i WHERE i.stock <= i.alertLevel")
    Long countLowStockGlobal();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM Inventory i WHERE i.storeId = :storeId AND i.stock <= i.alertLevel")
    Long countLowStockByStore(Long storeId);
}
