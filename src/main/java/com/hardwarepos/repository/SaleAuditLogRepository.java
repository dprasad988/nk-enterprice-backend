package com.hardwarepos.repository;

import com.hardwarepos.entity.SaleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleAuditLogRepository extends JpaRepository<SaleAuditLog, Long> {
    Page<SaleAuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<SaleAuditLog> findAllByStoreIdOrderByTimestampDesc(Long storeId, Pageable pageable);
    
    Page<SaleAuditLog> findAllByTimestampBetweenOrderByTimestampDesc(java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);
    Page<SaleAuditLog> findAllByStoreIdAndTimestampBetweenOrderByTimestampDesc(Long storeId, java.time.LocalDateTime start, java.time.LocalDateTime end, Pageable pageable);
}
