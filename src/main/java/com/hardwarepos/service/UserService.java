package com.hardwarepos.service;

import com.hardwarepos.entity.User;
import com.hardwarepos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Clear passwords before sending to frontend
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public User createUser(User user) {
        if (user.getRole() == User.Role.OWNER) {
            throw new RuntimeException("Cannot create additional Owner accounts.");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
