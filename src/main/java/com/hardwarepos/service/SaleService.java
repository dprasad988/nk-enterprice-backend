package com.hardwarepos.service;

import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.SaleItem;
import com.hardwarepos.entity.SaleVersion;
import com.hardwarepos.entity.SaleVersionItem;
import com.hardwarepos.entity.SaleAuditLog;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.DamageItemRepository;
import com.hardwarepos.repository.InventoryRepository;
import com.hardwarepos.repository.ProductRepository;
import com.hardwarepos.repository.SaleAuditLogRepository;
import com.hardwarepos.repository.SaleRepository;
import com.hardwarepos.repository.SaleVersionRepository;
import com.hardwarepos.repository.UserRepository;
import com.hardwarepos.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class SaleService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private SaleAuditLogRepository saleAuditLogRepository;



    @Autowired
    private SaleVersionRepository saleVersionRepository;

    @Autowired
    private DamageItemRepository damageItemRepository;

    private void logAction(String action, Sale sale, String details, User user) {
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

    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));
    }

    public Sale getExchangeableSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (sale.getSaleDate().isBefore(java.time.LocalDateTime.now().minusDays(3))) {
             throw new IllegalArgumentException("Exchange period expired (Limit: 3 days)");
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

    public List<Sale> getAllSales(User user, Long storeId) {
        if (user.getRole() == User.Role.OWNER) {
            if (storeId != null) {
                return saleRepository.findAllByStoreIdOrderBySaleDateDesc(storeId);
            }
            return saleRepository.findAllByOrderBySaleDateDesc();
        } else {
            return saleRepository.findAllByStoreIdOrderBySaleDateDesc(user.getStoreId());
        }
    }

    @Transactional
    public Sale createSale(User user, Sale sale) {
        sale.setSaleDate(LocalDateTime.now());
        sale.setCashierName(user.getUsername());
        
        if (user.getRole() != User.Role.OWNER && user.getStoreId() != null) {
            sale.setStoreId(user.getStoreId());
        } else if (sale.getStoreId() == null && user.getStoreId() != null) {
            sale.setStoreId(user.getStoreId());
        }
        
        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                item.setSale(sale);
                
                if (item.getProductId() != null) {
                    com.hardwarepos.entity.Inventory inventory = inventoryRepository.findByStoreIdAndProductId(sale.getStoreId(), item.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found in this store"));
                    
                    int newStock = inventory.getStock() - item.getQuantity();
                    if (newStock < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + inventory.getProduct().getName());
                    }
                    inventory.setStock(newStock);
                    inventoryRepository.save(inventory);

                    item.setCostPrice(inventory.getCostPrice() != null ? inventory.getCostPrice() : 0.0);
                    if (item.getProductName() == null || item.getProductName().isEmpty()) {
                        item.setProductName(inventory.getProduct().getName());
                    }
                }
                
                if (item.getPrice() != null && item.getQuantity() != null) {
                     item.setSubtotal(item.getPrice() * item.getQuantity());
                }
            }
        }
        
        if (sale.getPaymentMethod() != null) {
            String pm = sale.getPaymentMethod();
            
            if (pm.startsWith("VOUCHER_PARTIAL:")) {
                try {
                    String[] parts = pm.split("\\|");
                    String[] voucherParts = parts[0].split(":");
                    
                    String vCode = voucherParts[1].trim();
                    double deductAmount = Double.parseDouble(voucherParts[2].trim());
                    
                    com.hardwarepos.entity.Voucher voucher = this.voucherRepository.findByCode(vCode)
                            .orElseThrow(() -> new RuntimeException("Invalid Voucher Code"));
                            
                    if (voucher.getCurrentBalance() < deductAmount) {
                         throw new RuntimeException("Insufficient Voucher Balance for Partial Payment");
                    }
                    
                    voucher.setCurrentBalance(0.0);
                    voucher.setStatus("REDEEMED");
                    this.voucherRepository.save(voucher);
                    
                    String otherMethod = parts.length > 1 ? " + " + parts[1] : "";
                    sale.setPaymentMethod("VOUCHER (" + vCode + "): " + deductAmount + otherMethod);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Error processing partial voucher: " + e.getMessage());
                }
            } 
            else if (pm.startsWith("VOUCHER_CODE:")) {
                String vCode = pm.split(":")[1].trim();
                com.hardwarepos.entity.Voucher voucher = this.voucherRepository.findByCode(vCode)
                     .orElseThrow(() -> new RuntimeException("Invalid Voucher Code during Checkout"));
                     
                double deduction = sale.getTotalAmount();
                if (deduction > voucher.getCurrentBalance()) {
                    throw new RuntimeException("Insufficient Voucher Balance");
                }
                
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

    private void saveSaleVersion(Sale sale, String reason) {
        SaleVersion version = new SaleVersion();
        version.setSale(sale);
        version.setVersionAt(LocalDateTime.now());
        version.setVersionReason(reason);
        version.setTotalAmount(sale.getTotalAmount());
        version.setPaymentMethod(sale.getPaymentMethod());
        version.setCashierName(sale.getCashierName());
        version.setDiscount(sale.getDiscount());

        List<SaleVersionItem> versionItems = new ArrayList<>();
        if (sale.getItems() != null) {
            for (SaleItem item : sale.getItems()) {
                SaleVersionItem vItem = new SaleVersionItem();
                vItem.setSaleVersion(version);
                vItem.setProductId(item.getProductId());
                vItem.setProductName(item.getProductName());
                
                // Fetch barcode manually since SaleItem doesn't strictly have it separately? 
                // Or try to get it. Ideally SaleItem should have it, but if not we fetch.
                if (item.getProductId() != null) {
                     productRepository.findById(item.getProductId())
                         .ifPresent(p -> vItem.setBarcode(p.getBarcode()));
                }
                // Removed the line that overwrites it with empty string
                // vItem.setBarcode(barcode);

                vItem.setQuantity(item.getQuantity());
                vItem.setPrice(item.getPrice());
                vItem.setCostPrice(item.getCostPrice());
                vItem.setDiscount(item.getDiscount());
                vItem.setSubtotal(item.getSubtotal());
                versionItems.add(vItem);
            }
        }
        version.setItems(versionItems);
        saleVersionRepository.save(version);
    }

    @Transactional
    public Sale updateSale(User user, Long id, Sale updatedSale) {
        Sale existingSale = saleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        // SNAPSHOT OLD STATE FOR HISTORY
        // Check if this is the FIRST update. If so, logic implies the CURRENT state is the "ORIGINAL".
        // Or "PRE-UPDATE 1". 
        // We just save the current state of 'existingSale' as a version.
        saveSaleVersion(existingSale, "PRE-UPDATE / EXCHANGE");
        
        // Restore stock logic (RETURN Items logic)
        if (existingSale.getItems() != null) {
            for (SaleItem oldItem : existingSale.getItems()) {
                if (oldItem.getProductId() != null) {
                    com.hardwarepos.entity.Inventory inventory = inventoryRepository.findByStoreIdAndProductId(existingSale.getStoreId(), oldItem.getProductId())
                            .orElse(null);
                    
                    if (inventory != null) {
                        inventory.setStock(inventory.getStock() + oldItem.getQuantity());
                        inventoryRepository.save(inventory);
                    }
                }
            }
        }

        if (updatedSale.getItems() != null) {
            for (SaleItem newItem : updatedSale.getItems()) {
                newItem.setSale(existingSale);
                
                if (newItem.getProductId() != null) {
                    com.hardwarepos.entity.Inventory inventory = inventoryRepository.findByStoreIdAndProductId(existingSale.getStoreId(), newItem.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found in this store: " + newItem.getProductId()));

                    int newStock = inventory.getStock() - newItem.getQuantity();
                    if (newStock < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + inventory.getProduct().getName());
                    }
                    inventory.setStock(newStock);
                    inventoryRepository.save(inventory);

                    newItem.setCostPrice(inventory.getCostPrice() != null ? inventory.getCostPrice() : 0.0);
                }
                
                if (newItem.getPrice() != null && newItem.getQuantity() != null) {
                        newItem.setSubtotal(newItem.getPrice() * newItem.getQuantity());
                }
            }
        }

        if (updatedSale.getTotalAmount() < existingSale.getTotalAmount()) {
             throw new RuntimeException("Exchange Error: New total (" + updatedSale.getTotalAmount() + 
                     ") cannot be less than original total (" + existingSale.getTotalAmount() + "). Customer must add more items.");
        }

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
                    
                    voucher.setCurrentBalance(0.0);
                    voucher.setStatus("REDEEMED");
                    this.voucherRepository.save(voucher);
                    
                    String otherMethod = parts.length > 1 ? " + " + parts[1] : "";
                    existingSale.setPaymentMethod("VOUCHER (" + vCode + "): " + deductAmount + otherMethod);
                    updatedSale.setPaymentMethod(existingSale.getPaymentMethod()); 

                } catch (Exception e) {
                    throw new RuntimeException("Error processing voucher: " + e.getMessage());
                }
            } else if (pm.startsWith("VOUCHER_CODE:")) {
                String vCode = pm.split(":")[1].trim();
                com.hardwarepos.entity.Voucher voucher = this.voucherRepository.findByCode(vCode)
                     .orElseThrow(() -> new RuntimeException("Invalid Voucher Code during Exchange"));
                     
                double deduction = existingSale.getTotalAmount();
                if (voucher.getCurrentBalance() < deduction) {
                    throw new RuntimeException("Insufficient Voucher Balance");
                }
                
                voucher.setCurrentBalance(0.0);
                voucher.setStatus("REDEEMED");
                this.voucherRepository.save(voucher);
                
                existingSale.setPaymentMethod("VOUCHER (" + vCode + ")");
                updatedSale.setPaymentMethod(existingSale.getPaymentMethod());
            }
        }

        existingSale.setTotalAmount(updatedSale.getTotalAmount());
        existingSale.setPaymentMethod(updatedSale.getPaymentMethod());
        existingSale.setCashierName(user.getUsername());
        existingSale.setSaleDate(LocalDateTime.now());
        
        existingSale.getItems().clear();
        if (updatedSale.getItems() != null) {
            existingSale.getItems().addAll(updatedSale.getItems());
        }

        Sale savedSale = saleRepository.save(existingSale);
        logAction("EXCHANGE", savedSale, "Sale Exchanged/Updated. New Total: " + savedSale.getTotalAmount(), user);
        return savedSale;
    }

    public Page<SaleAuditLog> getSaleLogs(User user, int page, int size, String startDate, String endDate, Long storeId) {
        Long targetStoreId = (user.getRole() == User.Role.OWNER) ? storeId : user.getStoreId();
        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null) {
            try {
                LocalDateTime start = LocalDateTime.parse(startDate);
                LocalDateTime end = LocalDateTime.parse(endDate);
                
                if (targetStoreId != null) {
                    return saleAuditLogRepository.findAllByStoreIdAndTimestampBetweenOrderByTimestampDesc(targetStoreId, start, end, pageable);
                } else {
                    return saleAuditLogRepository.findAllByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
                }
            } catch (Exception e) {
               // Log error
            }
        }
        
        if (targetStoreId != null) {
            return saleAuditLogRepository.findAllByStoreIdOrderByTimestampDesc(targetStoreId, pageable);
        } else {
            return saleAuditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
    }

    public List<SaleVersion> getSaleVersions(Long saleId) {
        return saleVersionRepository.findBySaleIdOrderByVersionAtDesc(saleId);
    }
}
