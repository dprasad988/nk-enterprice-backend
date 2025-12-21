package com.hardwarepos.repository;

import com.hardwarepos.entity.DamageItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DamageItemRepository extends JpaRepository<DamageItem, Long> {
    List<DamageItem> findByOriginalSaleId(Long originalSaleId);
    List<DamageItem> findByStatus(String status);
    List<DamageItem> findByStatusAndStoreId(String status, Long storeId);
    List<DamageItem> findByStoreId(Long storeId);
}
