package com.hardwarepos.controller;

import com.hardwarepos.entity.AppSetting;
import com.hardwarepos.repository.AppSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "*")
public class AppSettingController {

    @Autowired
    private AppSettingRepository appSettingRepository;

    @GetMapping
    public List<AppSetting> getAllSettings() {
        return appSettingRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<List<AppSetting>> updateSettings(@RequestBody List<AppSetting> settings) {
        List<AppSetting> saved = appSettingRepository.saveAll(settings);
        return ResponseEntity.ok(saved);
    }
    
    // Helper to get settings as map for simpler frontend consumption if needed
    @GetMapping("/map")
    public Map<String, String> getSettingsMap() {
        return appSettingRepository.findAll().stream()
                .collect(Collectors.toMap(AppSetting::getSettingKey, AppSetting::getSettingValue));
    }
}
