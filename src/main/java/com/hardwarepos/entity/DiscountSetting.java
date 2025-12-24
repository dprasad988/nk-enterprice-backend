package com.hardwarepos.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "discount_settings")
@IdClass(DiscountSettingId.class)
public class DiscountSetting {

    @JsonProperty("settingKey")
    @Id
    private String settingKey; 
    
    @JsonProperty("storeId")
    @Id
    private Long storeId;

    @JsonProperty("settingValue")
    private String settingValue;
    
    @JsonProperty("description")
    private String description;

    public DiscountSetting() {}

    public DiscountSetting(String settingKey, Long storeId, String settingValue, String description) {
        this.settingKey = settingKey;
        this.storeId = storeId;
        this.settingValue = settingValue;
        this.description = description;
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getSettingValue() { return settingValue; }
    public void setSettingValue(String settingValue) { this.settingValue = settingValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "DiscountSetting{key='" + settingKey + "', val='" + settingValue + "', store=" + storeId + "}";
    }
}
