package com.hardwarepos.service;

import com.hardwarepos.entity.DiscountSetting;
import com.hardwarepos.entity.DiscountSettingId;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.DiscountSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DiscountSettingService {

    @Autowired
    private DiscountSettingRepository discountSettingRepository;

    @Autowired
    private SecurityService securityService;

    public List<DiscountSetting> getSettingsByStore(User user, Long storeId) {
        Long targetStoreId = securityService.resolveTargetStoreId(user, storeId);
        if (targetStoreId == null) {
             return java.util.Collections.emptyList();
        }
        return discountSettingRepository.findByStoreId(targetStoreId);
    }

    public List<DiscountSetting> updateSettings(User user, List<DiscountSetting> settings) {
        List<DiscountSetting> toSave = new java.util.ArrayList<>();

        for (DiscountSetting setting : settings) {
            Long targetStoreId = securityService.resolveTargetStoreId(user, setting.getStoreId());
             if (targetStoreId == null) {
                  throw new RuntimeException("Store Context Missing for Setting Update. " +
                          "If you are an Owner, ensure a store is selected in the content/header." +
                          " settingKey=" + setting.getSettingKey());
             }
             setting.setStoreId(targetStoreId);
             
             // Upsert Logic
             DiscountSettingId id = new DiscountSettingId(setting.getSettingKey(), targetStoreId);
             Optional<DiscountSetting> existingParams = discountSettingRepository.findById(id);
             
             if (existingParams.isPresent()) {
                 DiscountSetting existing = existingParams.get();
                 existing.setSettingValue(setting.getSettingValue());
                 existing.setDescription(setting.getDescription());
                 toSave.add(existing);
             } else {
                 toSave.add(setting);
             }
        }
        
        return discountSettingRepository.saveAll(toSave);
    }

    public Map<String, String> getSettingsMap(User user, Long storeId) {
        List<DiscountSetting> settings = getSettingsByStore(user, storeId);
        return settings.stream()
                .collect(Collectors.toMap(DiscountSetting::getSettingKey, DiscountSetting::getSettingValue));
    }
}
