package com.hardwarepos.controller;

import com.hardwarepos.dto.AuthRequest;
import com.hardwarepos.dto.AuthResponse;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.UserRepository;
import com.hardwarepos.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.hardwarepos.entity.UserSession;
import com.hardwarepos.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hardwarepos.repository.StoreRepository storeRepository;
    
    @Autowired
    private UserSessionRepository userSessionRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            String token = jwtUtil.generateToken(authRequest.getUsername());
            
            // Track Session
            try {
                UserSession session = new UserSession();
                session.setUserId(user.getId());
                session.setUsername(user.getUsername());
                session.setIpAddress(request.getRemoteAddr());
                session.setUserAgent(request.getHeader("User-Agent"));
                session.setLoginTime(LocalDateTime.now());
                session.setStatus("ACTIVE");
                userSessionRepository.save(session);
            } catch (Exception e) {
                System.err.println("Failed to save session: " + e.getMessage());
            }
            
            String storeName = "Global Access";
            if (user.getStoreId() != null) {
                 storeName = storeRepository.findById(user.getStoreId())
                         .map(com.hardwarepos.entity.Store::getName)
                         .orElse("Unknown Store");
            }

            return ResponseEntity.ok(new AuthResponse(
                    token, 
                    user.getUsername(), 
                    user.getRole().toString(), 
                    user.getStoreId(),
                    storeName
            ));
        } else {
            throw new UsernameNotFoundException("Invalid user request !");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            
            userRepository.findByUsername(username).ifPresent(user -> {
                userSessionRepository.findFirstByUserIdAndStatusOrderByLoginTimeDesc(user.getId(), "ACTIVE")
                    .ifPresent(session -> {
                        session.setLogoutTime(LocalDateTime.now());
                        session.setStatus("CLOSED");
                        userSessionRepository.save(session);
                    });
            });
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}
