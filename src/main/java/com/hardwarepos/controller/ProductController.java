package com.hardwarepos.controller;

import com.hardwarepos.entity.Product;
import com.hardwarepos.entity.Inventory;
import com.hardwarepos.repository.ProductRepository;
import com.hardwarepos.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.hardwarepos.entity.ProductAuditLog;
import java.time.LocalDateTime;


import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private com.hardwarepos.repository.UserRepository userRepository;

    @Autowired
    private com.hardwarepos.repository.ProductAuditLogRepository auditLogRepository;




    private com.hardwarepos.entity.User getAuthenticatedUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void logAction(String action, Inventory inventory, String details, com.hardwarepos.entity.User user) {
        ProductAuditLog log = new ProductAuditLog();
        log.setAction(action);
        log.setProductId(inventory.getProduct().getId());
        log.setProductName(inventory.getProduct().getName());
        log.setStoreId(inventory.getStoreId()); // Access StoreId from Inventory
        log.setActionBy(user.getUsername());
        log.setTimestamp(LocalDateTime.now());
        log.setDetails(details);
        auditLogRepository.save(log);
    }


    // DTO to match the old Product structure for frontend compatibility
    static class ProductDTO {
        private Long id; // Inventory ID (to allow editing specific store inventory)
        private Long productId; // Global Product ID
        private Long storeId;
        private String name;
        private String barcode;
        private String sku;
        private Double price;
        private Double costPrice;
        private Integer stock;
        private Double discount;
        private Integer alertLevel;
// private com.hardwarepos.entity.Category category; // Removed

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBarcode() { return barcode; }
        public void setBarcode(String barcode) { this.barcode = barcode; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Double getCostPrice() { return costPrice; }
        public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Double getDiscount() { return discount; }
        public void setDiscount(Double discount) { this.discount = discount; }
        public Integer getAlertLevel() { return alertLevel; }
        public void setAlertLevel(Integer alertLevel) { this.alertLevel = alertLevel; }
// Getters/Setters for category removed

        public static ProductDTO from(Inventory inv) {
            ProductDTO dto = new ProductDTO();
            dto.setId(inv.getProduct().getId()); 
            
            dto.setProductId(inv.getProduct().getId());
            dto.setStoreId(inv.getStoreId());
            dto.setName(inv.getProduct().getName());
            dto.setBarcode(inv.getProduct().getBarcode());
            dto.setSku(inv.getProduct().getSku());
            // dto.setCategory(inv.getProduct().getCategory()); // Removed
            
            dto.setPrice(inv.getPrice());
            dto.setCostPrice(inv.getCostPrice());
            dto.setStock(inv.getStock());
            dto.setDiscount(inv.getDiscount());
            dto.setAlertLevel(inv.getAlertLevel());
            return dto;
        }

        @Override
        public String toString() {
            return "ProductDTO{" +
                    "id=" + id +
                    ", productId=" + productId +
                    ", storeId=" + storeId +
                    ", name='" + name + '\'' +
                    ", barcode='" + barcode + '\'' +
                    ", sku='" + sku + '\'' +
                    ", price=" + price +
                    ", costPrice=" + costPrice +
                    ", stock=" + stock +
                    ", discount=" + discount +
                    ", alertLevel=" + alertLevel +
//                    ", category=" + category +
                    '}';
        }
    }

    @GetMapping
    public List<ProductDTO> getAllProducts(@RequestParam(required = false) Long storeId) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId;

        if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
            targetStoreId = storeId; // Owner can request specific store
        } else {
            targetStoreId = user.getStoreId(); // Others locked to their store
        }

        if (targetStoreId == null) {
            // If owner asks for all products without store filter, 
            // techincally we should show all inventory items across all stores?
            // Or just unique products? 
            // For now, let's return all inventory items.
             return inventoryRepository.findAll().stream()
                .map(ProductDTO::from)
                .collect(Collectors.toList());
        }

        return inventoryRepository.findAllByStoreId(targetStoreId).stream()
            .map(ProductDTO::from)
            .collect(Collectors.toList());
    }

    @GetMapping("/paged")
    public org.springframework.data.domain.Page<ProductDTO> getProductsPaged(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId;

        if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
            targetStoreId = storeId;
        } else {
            targetStoreId = user.getStoreId();
        }

        if (targetStoreId == null) {
            return org.springframework.data.domain.Page.empty();
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Inventory> result;

        if (search != null && !search.trim().isEmpty()) {
             result = inventoryRepository.searchByStore(targetStoreId, search.trim(), pageable);
        } else {
             result = inventoryRepository.findAllByStoreId(targetStoreId, pageable);
        }

        return result.map(ProductDTO::from);
    }

    @GetMapping("/search")
    public List<ProductDTO> searchProducts(@RequestParam String query) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long storeId = user.getStoreId();
        
        // This is tricky. We need to search products by name, then find their inventory for THIS store.
        if (storeId != null) {
            // Use the optimized repository method with pagination (or a list variant if needed)
            // For now, let's reuse searchByStore which returns a Page. 
            // We can just ask for a large page to simulate "List" behavior for this endpoint 
            // OR create a List-returning variant in repo. 
            // Given the existing signature returns List, let's just use Page request with size 50.
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 50);
            return inventoryRepository.searchByStore(storeId, query, pageable)
                    .map(ProductDTO::from)
                    .getContent();
        } else {
             // For global/owner search, we can fallback to product repository but we might still hit N+1 
             // if we then try to fetch inventory. 
             // But the original code just returned products mapped to empty DTOs? 
             // "flatMap(p -> java.util.stream.Stream.of(new ProductDTO()))" -> this was broken anyway.
             // Let's just return products found in ProductRepository as basic DTOs without inventory info.
             List<Product> products = productRepository.findByNameContainingIgnoreCase(query);
             return products.stream().map(p -> {
                 ProductDTO dto = new ProductDTO();
                 dto.setProductId(p.getId());
                 dto.setName(p.getName());
                 dto.setBarcode(p.getBarcode());
                 dto.setSku(p.getSku());
                 // dto.setCategory(p.getCategory());
                 return dto;
             }).collect(Collectors.toList());
        }

    }
    
    @PostMapping
    public ProductDTO addProduct(@RequestBody ProductDTO payload) {
        System.out.println("Received Add Product Payload: " + payload);
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId = (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) ? payload.getStoreId() : user.getStoreId();

        if (targetStoreId == null) {
             throw new RuntimeException("Store ID is required to add inventory.");
        }

        // 1. Check if Product exists globally
        Optional<Product> existingProduct = productRepository.findByBarcode(payload.getBarcode());
        Product product;
        
        if (existingProduct.isPresent()) {
            product = existingProduct.get();
        } else {
            // Create Global Product
            product = new Product();
            product.setName(payload.getName());
            product.setBarcode(payload.getBarcode());
            product.setSku(payload.getSku());
            // product.setCategory(payload.getCategory());
            product = productRepository.save(product);
        }

        // 2. Check if Inventory exists for this store
        if (inventoryRepository.findByStoreIdAndProductId(targetStoreId, product.getId()).isPresent()) {
            throw new RuntimeException("Product already exists in this store.");
        }

        // 3. Create Inventory
        Inventory inventory = new Inventory();
        inventory.setStoreId(targetStoreId);
        inventory.setProduct(product);
        inventory.setStock(payload.getStock());
        inventory.setPrice(payload.getPrice());
        inventory.setCostPrice(payload.getCostPrice());
        inventory.setDiscount(payload.getDiscount());
        inventory.setAlertLevel(payload.getAlertLevel());
        
        inventory = inventoryRepository.save(inventory);

        logAction("ADD", inventory, "Product created with stock: " + payload.getStock() + ", Price: " + payload.getPrice(), user);

        return ProductDTO.from(inventory);
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(@PathVariable Long id, @RequestBody ProductDTO payload) {
        // id here is ProductID based on our DTO mapping
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId = (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) ? payload.getStoreId() : user.getStoreId();
        
        if (targetStoreId == null) throw new RuntimeException("Store context required.");

        Inventory inventory = inventoryRepository.findByStoreIdAndProductId(targetStoreId, id)
             .orElseThrow(() -> new RuntimeException("Product not found in this store"));

        // Capture Old Values for Logging
        StringBuilder changes = new StringBuilder();
        
        if (!java.util.Objects.equals(inventory.getStock(), payload.getStock())) {
            changes.append("Stock: ").append(inventory.getStock()).append("->").append(payload.getStock()).append(", ");
        }
        if (!java.util.Objects.equals(inventory.getPrice(), payload.getPrice())) {
            changes.append("Price: ").append(inventory.getPrice()).append("->").append(payload.getPrice()).append(", ");
        }
        if (!java.util.Objects.equals(inventory.getCostPrice(), payload.getCostPrice())) {
            changes.append("Cost: ").append(inventory.getCostPrice()).append("->").append(payload.getCostPrice()).append(", ");
        }
        if (!java.util.Objects.equals(inventory.getDiscount(), payload.getDiscount())) {
            changes.append("Discount: ").append(inventory.getDiscount()).append("->").append(payload.getDiscount()).append(", ");
        }
        if (!java.util.Objects.equals(inventory.getAlertLevel(), payload.getAlertLevel())) {
            changes.append("Alert Level: ").append(inventory.getAlertLevel()).append("->").append(payload.getAlertLevel()).append(", ");
        }
        
        Product product = inventory.getProduct();
        if (payload.getName() != null && !payload.getName().equals(product.getName())) {
             changes.append("Name: ").append(product.getName()).append("->").append(payload.getName()).append(", ");
        }
        if (payload.getBarcode() != null && !payload.getBarcode().equals(product.getBarcode())) {
             changes.append("Barcode: ").append(product.getBarcode()).append("->").append(payload.getBarcode()).append(", ");
        }

        // Update Entity
        inventory.setStock(payload.getStock());
        inventory.setPrice(payload.getPrice());
        inventory.setCostPrice(payload.getCostPrice());
        inventory.setDiscount(payload.getDiscount());
        inventory.setAlertLevel(payload.getAlertLevel());
        
        product.setName(payload.getName());
        product.setBarcode(payload.getBarcode()); // Enabled barcode update
        
        try {
            productRepository.save(product);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
             throw new RuntimeException("Barcode already exists.");
        }
        inventory = inventoryRepository.save(inventory);

        // If nothing changed, we might still log or just skip. 
        // User clicked save, so let's log "Updated" even if no fields changed, or "No changes".
        String changeDetails = changes.length() > 0 ? changes.substring(0, changes.length() - 2) : "No specific changes detected";

        logAction("EDIT", inventory, changeDetails, user);

        return ProductDTO.from(inventory);
    }

    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public void deleteProduct(@PathVariable Long id, @RequestParam(required = false) Long storeId) {
        System.out.println("Processing Delete Request for Product ID: " + id + ", StoreID Param: " + storeId);

        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId;

        if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
             targetStoreId = storeId;
        } else {
            targetStoreId = user.getStoreId();
        }
        
        System.out.println("Target Store ID: " + targetStoreId);
        
        if (targetStoreId == null) {
            throw new RuntimeException("Store context required for deletion.");
        }

        // Check availability before delete
        Inventory inventory = inventoryRepository.findByStoreIdAndProductId(targetStoreId, id)
             .orElseThrow(() -> new RuntimeException("Inventory item not found for Store " + targetStoreId + " Product " + id));
             
        System.out.println("Found Inventory ID: " + inventory.getId() + " - Stock: " + inventory.getStock());
        
        long countBefore = inventoryRepository.count();
        System.out.println("Total Inventory Count Before: " + countBefore);

        inventoryRepository.delete(inventory);
        inventoryRepository.flush(); // FORCE FLUSH
        
        logAction("DELETE", inventory, "Product deleted. Final Stock: " + inventory.getStock(), user);

        
        long countAfter = inventoryRepository.count();
        System.out.println("Total Inventory Count After: " + countAfter);
        
        if (countAfter >= countBefore) {
             System.out.println("CRITICAL WARNING: Delete called but count did not decrease!");
        } else {
             System.out.println("Deletion appears successful. Count decreased.");
        }
    }
    @GetMapping("/logs")
    public org.springframework.data.domain.Page<ProductAuditLog> getProductLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long storeId
    ) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId;
        
        if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
            targetStoreId = storeId; // Null implies all stores
        } else {
            targetStoreId = user.getStoreId();
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        if (startDate != null && endDate != null) {
            try {
                java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
                
                if (targetStoreId != null) {
                    return auditLogRepository.findAllByStoreIdAndTimestampBetweenOrderByTimestampDesc(targetStoreId, start, end, pageable);
                } else {
                    return auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
                }
            } catch (Exception e) {
                System.err.println("Date parse error: " + e.getMessage());
            }
        }
        
        if (targetStoreId != null) {
            return auditLogRepository.findAllByStoreIdOrderByTimestampDesc(targetStoreId, pageable);
        } else {
            return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
    }
}

