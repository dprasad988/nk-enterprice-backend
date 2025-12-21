package com.hardwarepos.controller;

import com.hardwarepos.entity.Purchase;
import com.hardwarepos.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*")
public class PurchaseController {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @GetMapping
    public List<Purchase> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    @PostMapping
    public Purchase createPurchase(@RequestBody Purchase purchase) {
        if (purchase.getPurchaseDate() == null) {
            purchase.setPurchaseDate(LocalDateTime.now());
        }
        // In a real app, this should also iterate items and increase Product stock.
        // For this iteration, we accept the purchase record. 
        // Logic to increase stock 'Product.stock += quantity' should be added here if Purchase had items.
        // But Purchase entity defined earlier was simple (Total Cost only).
        // I will keep it simple for now as per entity definition.
        return purchaseRepository.save(purchase);
    }
}
