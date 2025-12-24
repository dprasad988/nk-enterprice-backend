package com.hardwarepos.entity;

import java.io.Serializable;
import java.util.Objects;

public class DiscountSettingId implements Serializable {
    private String settingKey;
    private Long storeId;

    public DiscountSettingId() {}

    public DiscountSettingId(String settingKey, Long storeId) {
        this.settingKey = settingKey;
        this.storeId = storeId;
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscountSettingId that = (DiscountSettingId) o;
        return Objects.equals(settingKey, that.settingKey) &&
               Objects.equals(storeId, that.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settingKey, storeId);
    }
}
