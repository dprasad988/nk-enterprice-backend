package com.hardwarepos.controller;

import com.hardwarepos.entity.Product;
import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.SaleItem;
import com.hardwarepos.entity.SaleAuditLog;
import com.hardwarepos.repository.ProductRepository;
import com.hardwarepos.repository.SaleRepository;
import com.hardwarepos.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SaleController {

    @Autowired
    private SaleRepository saleRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private com.hardwarepos.repository.UserRepository userRepository;

    @Autowired
    private com.hardwarepos.repository.VoucherRepository voucherRepository;

    @Autowired
    private com.hardwarepos.repository.SaleAuditLogRepository saleAuditLogRepository;

    @Autowired
    private com.hardwarepos.repository.DamageItemRepository damageItemRepository;

    private com.hardwarepos.entity.User getAuthenticatedUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void logAction(String action, Sale sale, String details, com.hardwarepos.entity.User user) {
        SaleAuditLog log = new SaleAuditLog();
        log.setAction(action);
        log.setSaleId(sale.getId());
        log.setBillNo(String.valueOf(sale.getId())); // Using ID as Bill No for now
        log.setStoreId(sale.getStoreId());
        log.setActionBy(user.getUsername());
        log.setTimestamp(LocalDateTime.now());
        log.setDetails(details);
        saleAuditLogRepository.save(log);
    }

    @GetMapping("/{id}")
    public Sale getSaleById(@PathVariable Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    @GetMapping("/{id}/exchangeable")
    public Sale getExchangeableSale(@PathVariable Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (sale.getSaleDate().isBefore(java.time.LocalDateTime.now().minusDays(3))) {
             throw new RuntimeException("Exchange period expired (Limit: 3 days)");
        }

        if (sale.getItems() != null) {
            java.util.List<com.hardwarepos.entity.DamageItem> damageItems = damageItemRepository.findByOriginalSaleId(id);
            java.util.Map<Long, Integer> returnedQuantities = new java.util.HashMap<>();
            
            for (com.hardwarepos.entity.DamageItem di : damageItems) {
                // If Sale was updated AFTER the return request, assume the return was handled/resolved in the update.
                if (sale.getUpdatedAt() != null && di.getReturnDate() != null && sale.getUpdatedAt().isAfter(di.getReturnDate())) {
                    continue;
                }

                if (!"REJECTED".equals(di.getStatus())) {
                    returnedQuantities.merge(di.getProductId(), di.getQuantity(), Integer::sum);
                }
            }

            // Safe robust iteration using ArrayList copy if needed, but Iterator on PersistentBag usually OK if not flushing
            java.util.Iterator<SaleItem> iterator = sale.getItems().iterator();
            while (iterator.hasNext()) {
                SaleItem item = iterator.next();
                int returned = returnedQuantities.getOrDefault(item.getProductId(), 0);
                if (returned > 0) {
                    int available = item.getQuantity() - returned;
                    if (available <= 0) {
                        iterator.remove();
                    } else {
                        item.setQuantity(available);
                    }
                }
            }
        }
        return sale;
    }

    @GetMapping
    public List<Sale> getAllSales(@RequestParam(required = false) Long storeId) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();

        if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
            if (storeId != null) {
                return saleRepository.findAllByStoreIdOrderBySaleDateDesc(storeId);
            }
            return saleRepository.findAllByOrderBySaleDateDesc();
        } else {
            return saleRepository.findAllByStoreIdOrderBySaleDateDesc(user.getStoreId());
        }
    }

    @PostMapping
    @Transactional
    public Sale createSale(@RequestBody Sale sale) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        
        sale.setSaleDate(LocalDateTime.now());
        sale.setCashierName(user.getUsername());
        
        if (user.getRole() != com.hardwarepos.entity.User.Role.OWNER && user.getStoreId() != null) {
            sale.setStoreId(user.getStoreId());
        } else if (sale.getStoreId() == null && user.getStoreId() != null) {
            // Fallback for Owner if not manually set (though Owner usually doesn't sell)
            sale.setStoreId(user.getStoreId());
        }
        
        // Handle stock reduction
        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                item.setSale(sale); // Set bi-directional relationship
                
                if (item.getProductId() != null) {
                    // Update: Logic to use Inventory table
                    com.hardwarepos.entity.Inventory inventory = inventoryRepository.findByStoreIdAndProductId(sale.getStoreId(), item.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found in this store"));
                    
                    int newStock = inventory.getStock() - item.getQuantity();
                    if (newStock < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + inventory.getProduct().getName());
                    }
                    inventory.setStock(newStock);
                    inventoryRepository.save(inventory);

                    // Populate missing fields
                    item.setCostPrice(inventory.getCostPrice()); // Snapshot cost price
                    if (item.getProductName() == null || item.getProductName().isEmpty()) {
                        item.setProductName(inventory.getProduct().getName());
                    }
                }
                
                // Calculate Subtotal
                if (item.getPrice() != null && item.getQuantity() != null) {
                     item.setSubtotal(item.getPrice() * item.getQuantity());
                }
            }
        }
        
        // Handle Voucher Redemption (if present in payment info - usually handled by Frontend sending split or special flag)
        // Simplification: Frontend sends "VOUCHER:<CODE>" as Payment Method if fully paid by voucher, 
        // OR we can add a transient field to SaleDTO?
        // Since we bind to Sale Entity directly, let's look at `paymentMethod`.
        // Format assumption: "VOUCHER:V-12345678" or just "VOUCHER" (if logic is purely frontend verification).
        // SECURITY: Backend MUST deduct the voucher balance. 
        // We need the frontend to send the Code. 
        // Let's assume frontend appends code to paymentMethod string or we check a separate param (not in Entity).
        // Problem: Sale entity structure is fixed.
        // Quick Fix: Extract Code from paymentMethod string.
        
        // Handle Voucher Redemption
        if (sale.getPaymentMethod() != null) {
            String pm = sale.getPaymentMethod();
            
            if (pm.startsWith("VOUCHER_PARTIAL:")) {
                // Format: VOUCHER_PARTIAL:<CODE>:<AMOUNT>|...
                // Example: VOUCHER_PARTIAL:V-123:500.00|CASH
                try {
                    String[] parts = pm.split("\\|"); // Split voucher part from rest
                    String[] voucherParts = parts[0].split(":");
                    
                    String vCode = voucherParts[1].trim();
                    double deductAmount = Double.parseDouble(voucherParts[2].trim());
                    
                    com.hardwarepos.entity.Voucher voucher = this.voucherRepository.findByCode(vCode)
                            .orElseThrow(() -> new RuntimeException("Invalid Voucher Code"));
                            
                    if (voucher.getCurrentBalance() < deductAmount) {
                         throw new RuntimeException("Insufficient Voucher Balance for Partial Payment");
                    }
                    
                    // User Request: Expire immediately after use (One-time use)
                    voucher.setCurrentBalance(0.0);
                    voucher.setStatus("REDEEMED");
                    this.voucherRepository.save(voucher);
                    
                    // Create pretty string
                    String otherMethod = parts.length > 1 ? " + " + parts[1] : "";
                    sale.setPaymentMethod("VOUCHER (" + vCode + "): " + deductAmount + otherMethod);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Error processing partial voucher: " + e.getMessage());
                }
            } 
            else if (pm.startsWith("VOUCHER_CODE:")) {
                // Full Payment Logic
                String vCode = pm.split(":")[1].trim();
                com.hardwarepos.entity.Voucher voucher = this.voucherRepository.findByCode(vCode)
                     .orElseThrow(() -> new RuntimeException("Invalid Voucher Code during Checkout"));
                     
                double deduction = sale.getTotalAmount();
                if (deduction > voucher.getCurrentBalance()) {
                    throw new RuntimeException("Insufficient Voucher Balance");
                }
                
                // User Request: Expire immediately
                voucher.setCurrentBalance(0.0);
                voucher.setStatus("REDEEMED");
                this.voucherRepository.save(voucher);
                
                sale.setPaymentMethod("VOUCHER (" + vCode + ")");
            }
        } 
        
        Sale savedSale = saleRepository.save(sale);
        logAction("SALE", savedSale, "New Sale Created. Total: " + savedSale.getTotalAmount(), user);
        return savedSale;
    }
    


    @PutMapping("/{id}")
    @Transactional
    public Sale updateSale(@PathVariable Long id, @RequestBody Sale updatedSale) {
        Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        com.hardwarepos.entity.User user = getAuthenticatedUser();

        // 1. Revert Stock for existing items
        if (existingSale.getItems() != null) {
            for (SaleItem oldItem : existingSale.getItems()) {
                if (oldItem.getProductId() != null) {
                    com.hardwarepos.entity.Inventory inventory = inventoryRepository.findByStoreIdAndProductId(existingSale.getStoreId(), oldItem.getProductId())
                            .orElse(null); // If product deleted/moved, ignore
                    
                    if (inventory != null) {
                        inventory.setStock(inventory.getStock() + oldItem.getQuantity());
                        inventoryRepository.save(inventory);
                    }
                }
            }
        }

        // 2. Clear old items (OrphanRemoval should handle deletion if setup, but explicit clear is safer with manual list management)
        // JPA/Hibernate often needs careful handling of collection replacement.
        // We'll clear the collection and let Hibernate delete orphans IF 'orphanRemoval=true' is on Entity.
        // Let's check Sale entity. It has CascadeType.ALL. Default orphanRemoval is false.
        // We might need to manually delete them via repository if we replace the list.
        // Better: Clear the list and add new ones if we trust Hibernate. 
        // Or simpler: We are replacing the whole list.
        // Let's rely on basic replacement behavior but we MUST ensure orphanRemoval is true in Entity for clean cleanup.
        // For now, I'll update the Sale entity to ensure orphanRemoval is true in the next step if needed.
        // Assuming cascade ALL works for save, but might leave orphans in DB if not strict. 
        // Let's manually delete if possible? No, too complex here.
        // I'll set updated items.
        
        // Actually, to avoid issues, let's assume we are REPLACING the items. 
        // Ideally we should modify Sale.java to specific orphanRemoval=true.
        
        // 3. Process New Items (Deduct Stock)
        if (updatedSale.getItems() != null) {
            for (SaleItem newItem : updatedSale.getItems()) {
                newItem.setSale(existingSale); // Link to existing sale
                
                if (newItem.getProductId() != null) {
                    com.hardwarepos.entity.Inventory inventory = inventoryRepository.findByStoreIdAndProductId(existingSale.getStoreId(), newItem.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found in this store: " + newItem.getProductId()));

                    int newStock = inventory.getStock() - newItem.getQuantity();
                    if (newStock < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + inventory.getProduct().getName());
                    }
                    inventory.setStock(newStock);
                    inventoryRepository.save(inventory);
                }
            }
        }

        // 3.5 Validate No Refund Policy
        // If it's an update, we generally don't want to return cash.
        // The new total must be >= old total.
        // User Requirement: "system should never return cash, if final total less than previous payment customer should be add more items"
        if (updatedSale.getTotalAmount() < existingSale.getTotalAmount()) {
             throw new RuntimeException("Exchange Error: New total (" + updatedSale.getTotalAmount() + 
                     ") cannot be less than original total (" + existingSale.getTotalAmount() + "). Customer must add more items.");
        }

        // 4. Update Header Fields
        existingSale.setTotalAmount(updatedSale.getTotalAmount());
        existingSale.setPaymentMethod(updatedSale.getPaymentMethod());
        existingSale.setCashierName(user.getUsername()); // Update to current user
        existingSale.setSaleDate(LocalDateTime.now()); // Update timestamp to now? Or keep original? "Update bill" usually implies correction -> Maybe keep original date but update content? Or new transaction?
        // User said: "if the customer gets items after week to change". This is an EXCHANGE.
        // Usually an exchange happens NOW. So date should be NOW.
        // But Receipt ID stays same.
        
        // 5. Update Items List
        // We need to clear existing collection to trigger deletions if orphanRemoval is on. 
        existingSale.getItems().clear();
        if (updatedSale.getItems() != null) {
            existingSale.getItems().addAll(updatedSale.getItems());
        }

        // Handle Voucher for Exchange
        if (updatedSale.getPaymentMethod() != null && !updatedSale.getPaymentMethod().equals(existingSale.getPaymentMethod())) {
            String pm = updatedSale.getPaymentMethod();
            if (pm.startsWith("VOUCHER_PARTIAL:")) {
                try {
                    String[] parts = pm.split("\\|");
                    String[] voucherParts = parts[0].split(":");
                    String vCode = voucherParts[1].trim();
                    double deductAmount = Double.parseDouble(voucherParts[2].trim());
                    com.hardwarepos.entity.Voucher voucher = this.voucherRepository.findByCode(vCode)
                            .orElseThrow(() -> new RuntimeException("Invalid Voucher Code"));
                    
                    if (voucher.getCurrentBalance() < deductAmount) throw new RuntimeException("Insufficient Voucher Balance");
                    
                    // Expire immediately
                    voucher.setCurrentBalance(0.0);
                    voucher.setStatus("REDEEMED");
                    this.voucherRepository.save(voucher);
                    
                    String otherMethod = parts.length > 1 ? " + " + parts[1] : "";
                    existingSale.setPaymentMethod("VOUCHER (" + vCode + "): " + deductAmount + otherMethod);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing voucher: " + e.getMessage());
                }
            }
        }

        Sale savedSale = saleRepository.save(existingSale);
        logAction("EXCHANGE", savedSale, "Sale Exchanged/Updated. New Total: " + savedSale.getTotalAmount(), user);
        return savedSale;
    }

    @GetMapping("/logs")
    public org.springframework.data.domain.Page<SaleAuditLog> getSaleLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long storeId
    ) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        Long targetStoreId;
        
        if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
            targetStoreId = storeId;
        } else {
            targetStoreId = user.getStoreId();
        }
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        if (startDate != null && endDate != null) {
            try {
                java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
                
                if (targetStoreId != null) {
                    return saleAuditLogRepository.findAllByStoreIdAndTimestampBetweenOrderByTimestampDesc(targetStoreId, start, end, pageable);
                } else {
                    return saleAuditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
                }
            } catch (Exception e) {
                System.err.println("Date parse error: " + e.getMessage());
            }
        }
        
        if (targetStoreId != null) {
            return saleAuditLogRepository.findAllByStoreIdOrderByTimestampDesc(targetStoreId, pageable);
        } else {
            return saleAuditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
    }
}

