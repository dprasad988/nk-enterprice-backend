package com.hardwarepos.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private Long storeId;
    private String storeName;

    public AuthResponse(String token, String username, String role, Long storeId, String storeName) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.storeId = storeId;
        this.storeName = storeName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
}
