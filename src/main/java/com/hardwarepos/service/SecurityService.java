package com.hardwarepos.service;

import com.hardwarepos.entity.User;
import com.hardwarepos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    @Autowired
    private UserRepository userRepository;

    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    /**
     * Resolves the target store ID based on the user's role and the requested store ID.
     * Owners can request any store (or null for global).
     * Non-owners are restricted to their assigned store.
     */
    public Long resolveTargetStoreId(User user, Long requestedStoreId) {
        if (user.getRole() == User.Role.OWNER) {
            return requestedStoreId;
        }
        return user.getStoreId();
    }
}
