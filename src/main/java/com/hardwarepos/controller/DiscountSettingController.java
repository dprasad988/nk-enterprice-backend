package com.hardwarepos.controller;

import com.hardwarepos.dto.DiscountSettingDTO;
import com.hardwarepos.entity.DiscountSetting;
import com.hardwarepos.service.DiscountSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/discount-settings")
public class DiscountSettingController {

    private static final Logger logger = LoggerFactory.getLogger(DiscountSettingController.class);

    @Autowired
    private DiscountSettingService discountSettingService;

    @Autowired
    private com.hardwarepos.service.SecurityService securityService;

    @GetMapping
    public List<DiscountSetting> getAllSettings(@RequestParam(required = false) Long storeId) {
        return discountSettingService.getSettingsByStore(securityService.getAuthenticatedUser(), storeId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OWNER')")
    public ResponseEntity<List<DiscountSetting>> updateSettings(@RequestBody List<DiscountSettingDTO> settingsDtos) {
        List<DiscountSetting> settings = settingsDtos.stream().map(dto -> {
            DiscountSetting s = new DiscountSetting();
            s.setSettingKey(dto.getSettingKey());
            s.setSettingValue(dto.getSettingValue());
            s.setStoreId(dto.getStoreId());
            s.setDescription(dto.getDescription());
            return s;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(discountSettingService.updateSettings(securityService.getAuthenticatedUser(), settings));
    }
    
    @GetMapping("/map")
    public Map<String, String> getSettingsMap(@RequestParam(required = false) Long storeId) {
        return discountSettingService.getSettingsMap(securityService.getAuthenticatedUser(), storeId);
    }
}
