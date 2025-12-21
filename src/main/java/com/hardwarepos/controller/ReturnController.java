package com.hardwarepos.controller;

import com.hardwarepos.entity.DamageItem;
import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.SaleItem;
import com.hardwarepos.entity.Voucher;
import com.hardwarepos.repository.DamageItemRepository;
import com.hardwarepos.repository.SaleRepository;
import com.hardwarepos.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
@CrossOrigin(origins = "*")
public class ReturnController {

    @Autowired
    private DamageItemRepository damageItemRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private com.hardwarepos.repository.UserRepository userRepository;

    private com.hardwarepos.entity.User getAuthenticatedUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/request")
    @Transactional
    public List<DamageItem> createReturnRequest(@RequestBody List<Map<String, Object>> returnItems) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        List<DamageItem> createdItems = new java.util.ArrayList<>();

        for (Map<String, Object> itemData : returnItems) {
            Long saleId = Long.valueOf(itemData.get("saleId").toString());
            Long productId = Long.valueOf(itemData.get("productId").toString());
            int quantity = Integer.parseInt(itemData.get("quantity").toString());
            String reason = (String) itemData.get("reason");

            // Validate Sale Date (Status 3 day rule)
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new RuntimeException("Sale not found: " + saleId));

            if (sale.getSaleDate().isBefore(LocalDateTime.now().minusDays(3))) {
                throw new RuntimeException("Return policy expired (3 days). Sale Date: " + sale.getSaleDate());
            }

            // Find SaleItem to get Price
            SaleItem saleItem = sale.getItems().stream()
                    .filter(si -> si.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Item not found in sale"));

            double price = saleItem.getPrice();
            if (saleItem.getDiscount() != null && saleItem.getDiscount() > 0) {
                 price = price * (1 - saleItem.getDiscount() / 100.0);
            }
             
            double refundLine = price * quantity;

            // Save Damage Item Request
            DamageItem damage = new DamageItem();
            damage.setOriginalSaleId(saleId);
            damage.setProductId(productId);
            damage.setProductName(saleItem.getProductName());
            damage.setQuantity(quantity);
            damage.setReason(reason);
            damage.setRefundAmount(refundLine);
            damage.setReturnDate(LocalDateTime.now());
            if (user.getRole() == com.hardwarepos.entity.User.Role.OWNER) {
                damage.setReturnedBy("Owner");
            } else if (user.getRole() == com.hardwarepos.entity.User.Role.STORE_ADMIN) {
                damage.setReturnedBy("Store Admin");
            } else {
                damage.setReturnedBy(user.getUsername());
            }
            damage.setStatus("PENDING");
            damage.setStoreId(sale.getStoreId());
            
            createdItems.add(damageItemRepository.save(damage));
        }
        return createdItems;
    }

    @GetMapping("/pending")
    public List<DamageItem> getPendingRequests(@RequestParam(required = false) Long storeId) {
        // Ideally: return damageItemRepository.findByStatusAndStoreId("PENDING", storeId);
        // But for performance I should update repo. 
        // Assuming I'll update Repo to have findByStatusAndStoreId.
        if (storeId != null) {
            return damageItemRepository.findByStatusAndStoreId("PENDING", storeId);
        }
        return damageItemRepository.findByStatus("PENDING");
    }

    @GetMapping("/approved")
    public List<DamageItem> getApprovedRequests(@RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            return damageItemRepository.findByStatusAndStoreId("APPROVED", storeId);
        }
        return damageItemRepository.findByStatus("APPROVED");
    }

    @GetMapping("/all")
    public List<DamageItem> getAllRequests(@RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            return damageItemRepository.findByStoreId(storeId);
        }
        return damageItemRepository.findAll();
    }

    @PostMapping("/{id}/approve")
    public DamageItem approveRequest(@PathVariable Long id) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        // Check if user is OWNER or ADMIN?
        // if (user.getRole() != Role.OWNER) throw ... 

        DamageItem item = damageItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        if (!"PENDING".equals(item.getStatus())) {
            throw new RuntimeException("Item is not in PENDING state");
        }

        item.setStatus("APPROVED");
        item.setApprovedBy(user.getUsername());
        item.setApprovalDate(LocalDateTime.now());
        
        return damageItemRepository.save(item);
    }
    
    @PostMapping("/{id}/reject")
    public DamageItem rejectRequest(@PathVariable Long id) {
        com.hardwarepos.entity.User user = getAuthenticatedUser();
        DamageItem item = damageItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        item.setStatus("REJECTED");
        item.setApprovedBy(user.getUsername()); // Rejected by
        item.setApprovalDate(LocalDateTime.now());
        
        return damageItemRepository.save(item);
    }

    @PostMapping("/issue-voucher")
    @Transactional
    public Voucher issueVoucher(@RequestBody List<Long> damageItemIds) {
        if (damageItemIds == null || damageItemIds.isEmpty()) throw new RuntimeException("No items selected");

        double totalRefund = 0.0;
        Long refSaleId = null;

        for (Long id : damageItemIds) {
            DamageItem item = damageItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found: " + id));
            
            if (!"APPROVED".equals(item.getStatus())) {
                throw new RuntimeException("Item " + id + " is not APPROVED. Status: " + item.getStatus());
            }

            totalRefund += item.getRefundAmount();
            refSaleId = item.getOriginalSaleId();
            
            item.setStatus("VOUCHER_ISSUED");
            // item.setVoucherCode(...) - set later
        }

        Voucher voucher = new Voucher();
        voucher.setCode("V-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        voucher.setAmount(totalRefund);
        voucher.setCurrentBalance(totalRefund);
        voucher.setStatus("ACTIVE");
        voucher.setIssuedDate(LocalDateTime.now());
        voucher.setExpiryDate(LocalDateTime.now().plusMonths(6));
        voucher.setIssuedForReturnId(refSaleId);

        Voucher saved = voucherRepository.save(voucher);
        
        // Back-link voucher code to items? 
        for (Long id : damageItemIds) {
             DamageItem item = damageItemRepository.findById(id).get();
             item.setVoucherCode(saved.getCode());
             damageItemRepository.save(item);
        }
        
        return saved;
    }

    @GetMapping("/sale/{saleId}")
    public List<DamageItem> getReturnsBySaleId(@PathVariable Long saleId) {
        return damageItemRepository.findByOriginalSaleId(saleId);
    }
}
