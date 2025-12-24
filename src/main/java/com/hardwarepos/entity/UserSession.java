package com.hardwarepos.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String username;
    
    @Column(length = 1000)
    private String userAgent; // Browser info can be long
    
    private String ipAddress;
    
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    
    private String status; // ACTIVE, CLOSED
}
