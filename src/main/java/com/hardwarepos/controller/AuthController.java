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

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hardwarepos.repository.StoreRepository storeRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        if (authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            String token = jwtUtil.generateToken(authRequest.getUsername());
            
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
}
