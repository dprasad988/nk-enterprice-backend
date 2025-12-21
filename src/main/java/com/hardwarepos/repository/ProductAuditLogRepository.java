package com.hardwarepos.repository;

import com.hardwarepos.entity.ProductAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAuditLogRepository extends JpaRepository<ProductAuditLog, Long> {
    // We want logs ordered by latest first
    Page<ProductAuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<ProductAuditLog> findAllByStoreIdOrderByTimestampDesc(Long storeId, Pageable pageable);

    // Filter by date range
    Page<ProductAuditLog> findAllByTimestampBetweenOrderByTimestampDesc(java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);
    Page<ProductAuditLog> findAllByStoreIdAndTimestampBetweenOrderByTimestampDesc(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);
}
