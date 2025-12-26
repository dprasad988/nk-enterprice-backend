package com.hardwarepos.repository;

import com.hardwarepos.entity.SaleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SaleVersionRepository extends JpaRepository<SaleVersion, Long> {
    List<SaleVersion> findBySaleIdOrderByVersionAtDesc(Long saleId);
    // Find oldest to get original?
    List<SaleVersion> findBySaleIdOrderByVersionAtAsc(Long saleId);
}
