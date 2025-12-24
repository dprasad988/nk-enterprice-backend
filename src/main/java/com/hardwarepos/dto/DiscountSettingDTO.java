package com.hardwarepos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountSettingDTO {
    @JsonProperty("settingKey")
    private String settingKey;

    @JsonProperty("settingValue")
    private String settingValue;

    @JsonProperty("storeId")
    private Long storeId;

    @JsonProperty("description")
    private String description;
}
