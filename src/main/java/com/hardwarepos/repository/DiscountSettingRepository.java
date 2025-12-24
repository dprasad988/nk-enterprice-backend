package com.hardwarepos.repository;

import com.hardwarepos.entity.DiscountSetting;
import com.hardwarepos.entity.DiscountSettingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscountSettingRepository extends JpaRepository<DiscountSetting, DiscountSettingId> {
    List<DiscountSetting> findByStoreId(Long storeId);
}
