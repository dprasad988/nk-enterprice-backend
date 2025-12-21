package com.hardwarepos.config;

import com.hardwarepos.entity.Product;
import com.hardwarepos.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ProductRepository productRepository, 
                                   com.hardwarepos.repository.UserRepository userRepository,
                                   com.hardwarepos.repository.StoreRepository storeRepository,
                                   org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        return args -> {
            // Create Default Stores
            if (storeRepository.count() == 0) {
                com.hardwarepos.entity.Store kStore = new com.hardwarepos.entity.Store();
                kStore.setName("Kosgama");
                kStore.setAddress("Kosgama");
                kStore.setPhone("000-0000000");
                storeRepository.save(kStore);

                com.hardwarepos.entity.Store gStore = new com.hardwarepos.entity.Store();
                gStore.setName("Galle");
                gStore.setAddress("Galle");
                gStore.setPhone("000-0000000");
                storeRepository.save(gStore);
                System.out.println("Default Stores Created: Kosgama, Galle");
            }

            // Create Default Admin User if none exist
            if (userRepository.count() == 0) {
                com.hardwarepos.entity.User admin = new com.hardwarepos.entity.User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(com.hardwarepos.entity.User.Role.OWNER);
                admin.setStoreId(null); // Global access
                userRepository.save(admin);
                System.out.println("Default Admin User Created: admin / admin123");
            }
        };
    }
}
