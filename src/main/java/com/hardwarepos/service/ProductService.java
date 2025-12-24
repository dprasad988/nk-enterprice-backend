package com.hardwarepos.service;

import com.hardwarepos.dto.ProductDTO;
import com.hardwarepos.entity.Inventory;
import com.hardwarepos.entity.Product;
import com.hardwarepos.entity.ProductAuditLog;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.InventoryRepository;
import com.hardwarepos.repository.ProductAuditLogRepository;
import com.hardwarepos.repository.ProductRepository;
import com.hardwarepos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository; // Needed for user lookups if not passed fully

    @Autowired
    private ProductAuditLogRepository auditLogRepository;

    // Helper to log actions
    private void logAction(String action, Inventory inventory, String details, User user) {
        ProductAuditLog log = new ProductAuditLog();
        log.setAction(action);
        log.setProductId(inventory.getProduct().getId());
        log.setProductName(inventory.getProduct().getName());
        log.setStoreId(inventory.getStoreId());
        log.setActionBy(user.getUsername());
        log.setTimestamp(LocalDateTime.now());
        log.setDetails(details);
        auditLogRepository.save(log);
    }
    
    private Long resolveTargetStoreId(User user, Long requestedStoreId) {
        if (user.getRole() == User.Role.OWNER) {
            return requestedStoreId;
        }
        return user.getStoreId();
    }

    public List<ProductDTO> getAllProducts(User user, Long storeId) {
        Long targetStoreId = resolveTargetStoreId(user, storeId);

        if (targetStoreId == null) {
            return inventoryRepository.findAll().stream()
                    .map(ProductDTO::from)
                    .collect(Collectors.toList());
        }

        return inventoryRepository.findAllByStoreId(targetStoreId).stream()
                .map(ProductDTO::from)
                .collect(Collectors.toList());
    }

    public Page<ProductDTO> getProductsPaged(User user, Long storeId, int page, int size, String search) {
        Long targetStoreId = resolveTargetStoreId(user, storeId);

        if (targetStoreId == null && user.getRole() != User.Role.OWNER) {
             // Non-owners must have a store ID
             return Page.empty();
        }
        // Owner with null store ID -> Global view or empty? logic suggests empty in current impl
        if (targetStoreId == null) {
            return Page.empty();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Inventory> result;

        if (search != null && !search.trim().isEmpty()) {
            result = inventoryRepository.searchByStore(targetStoreId, search.trim(), pageable);
        } else {
            result = inventoryRepository.findAllByStoreId(targetStoreId, pageable);
        }

        return result.map(ProductDTO::from);
    }

    public List<ProductDTO> searchProducts(User user, String query) {
        Long storeId = user.getStoreId(); // Explicitly use user's store for search context if not owner-overridden? 
        // Existing logic used User's StoreID directly.
        
        if (storeId != null) {
            Pageable pageable = PageRequest.of(0, 50);
            return inventoryRepository.searchByStore(storeId, query, pageable)
                    .map(ProductDTO::from)
                    .getContent();
        } else {
            // Global search fallback
            List<Product> products = productRepository.findByNameContainingIgnoreCase(query);
            return products.stream().map(p -> {
                ProductDTO dto = new ProductDTO();
                dto.setProductId(p.getId());
                dto.setName(p.getName());
                dto.setBarcode(p.getBarcode());
                dto.setSku(p.getSku());
                return dto;
            }).collect(Collectors.toList());
        }
    }

    @Transactional
    public ProductDTO addProduct(User user, ProductDTO payload) {
        Long targetStoreId = (user.getRole() == User.Role.OWNER) ? payload.getStoreId() : user.getStoreId();

        if (targetStoreId == null) {
            throw new RuntimeException("Store ID is required to add inventory.");
        }

        Optional<Product> existingProduct = productRepository.findByBarcode(payload.getBarcode());
        Product product;

        if (existingProduct.isPresent()) {
            product = existingProduct.get();
        } else {
            product = new Product();
            product.setName(payload.getName());
            product.setBarcode(payload.getBarcode());
            product.setSku(payload.getSku());
            product = productRepository.save(product);
        }

        if (inventoryRepository.findByStoreIdAndProductId(targetStoreId, product.getId()).isPresent()) {
            throw new RuntimeException("Product already exists in this store.");
        }

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

    @Transactional
    public ProductDTO updateProduct(User user, Long productId, ProductDTO payload) {
        Long targetStoreId = (user.getRole() == User.Role.OWNER) ? payload.getStoreId() : user.getStoreId();

        if (targetStoreId == null) throw new RuntimeException("Store context required.");

        Inventory inventory = inventoryRepository.findByStoreIdAndProductId(targetStoreId, productId)
                .orElseThrow(() -> new RuntimeException("Product not found in this store"));

        StringBuilder changes = new StringBuilder();
        // Change detection logic
        if (!java.util.Objects.equals(inventory.getStock(), payload.getStock())) changes.append("Stock: ").append(inventory.getStock()).append("->").append(payload.getStock()).append(", ");
        if (!java.util.Objects.equals(inventory.getPrice(), payload.getPrice())) changes.append("Price: ").append(inventory.getPrice()).append("->").append(payload.getPrice()).append(", ");
        if (!java.util.Objects.equals(inventory.getCostPrice(), payload.getCostPrice())) changes.append("Cost: ").append(inventory.getCostPrice()).append("->").append(payload.getCostPrice()).append(", ");
        if (!java.util.Objects.equals(inventory.getDiscount(), payload.getDiscount())) changes.append("Discount: ").append(inventory.getDiscount()).append("->").append(payload.getDiscount()).append(", ");
        if (!java.util.Objects.equals(inventory.getAlertLevel(), payload.getAlertLevel())) changes.append("Alert Level: ").append(inventory.getAlertLevel()).append("->").append(payload.getAlertLevel()).append(", ");

        Product product = inventory.getProduct();
        if (payload.getName() != null && !payload.getName().equals(product.getName())) changes.append("Name: ").append(product.getName()).append("->").append(payload.getName()).append(", ");
        if (payload.getBarcode() != null && !payload.getBarcode().equals(product.getBarcode())) changes.append("Barcode: ").append(product.getBarcode()).append("->").append(payload.getBarcode()).append(", ");

        inventory.setStock(payload.getStock());
        inventory.setPrice(payload.getPrice());
        inventory.setCostPrice(payload.getCostPrice());
        inventory.setDiscount(payload.getDiscount());
        inventory.setAlertLevel(payload.getAlertLevel());

        product.setName(payload.getName());
        product.setBarcode(payload.getBarcode());

        try {
            productRepository.save(product);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException("Barcode already exists.");
        }
        inventory = inventoryRepository.save(inventory);

        String changeDetails = changes.length() > 0 ? changes.substring(0, changes.length() - 2) : "No specific changes detected";
        logAction("EDIT", inventory, changeDetails, user);

        return ProductDTO.from(inventory);
    }

    @Transactional
    public void deleteProduct(User user, Long productId, Long storeIdParam) {
        Long targetStoreId = (user.getRole() == User.Role.OWNER) ? storeIdParam : user.getStoreId();

        if (targetStoreId == null) {
            throw new RuntimeException("Store context required for deletion.");
        }

        Inventory inventory = inventoryRepository.findByStoreIdAndProductId(targetStoreId, productId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found for Store " + targetStoreId + " Product " + productId));

        inventoryRepository.delete(inventory);
        inventoryRepository.flush();

        logAction("DELETE", inventory, "Product deleted. Final Stock: " + inventory.getStock(), user);
    }

    public Page<ProductAuditLog> getProductLogs(User user, int page, int size, String startDate, String endDate, Long storeId) {
        Long targetStoreId = (user.getRole() == User.Role.OWNER) ? storeId : user.getStoreId();
        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);

                if (targetStoreId != null) {
                    return auditLogRepository.findAllByStoreIdAndTimestampBetweenOrderByTimestampDesc(targetStoreId, start, end, pageable);
                } else {
                    return auditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
                }
            } catch (Exception e) {
                // Log error
            }
        }

        if (targetStoreId != null) {
            return auditLogRepository.findAllByStoreIdOrderByTimestampDesc(targetStoreId, pageable);
        } else {
            return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
    }
}
