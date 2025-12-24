package com.hardwarepos.service;

import com.hardwarepos.entity.DamageItem;
import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.SaleItem;
import com.hardwarepos.entity.User;
import com.hardwarepos.entity.Voucher;
import com.hardwarepos.repository.DamageItemRepository;
import com.hardwarepos.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReturnService {

    @Autowired
    private DamageItemRepository damageItemRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private VoucherService voucherService;

    @Transactional
    public List<DamageItem> createReturnRequest(User user, List<Map<String, Object>> returnItems) {
        List<DamageItem> createdItems = new ArrayList<>();

        for (Map<String, Object> itemData : returnItems) {
            Long saleId = Long.valueOf(itemData.get("saleId").toString());
            Long productId = Long.valueOf(itemData.get("productId").toString());
            int quantity = Integer.parseInt(itemData.get("quantity").toString());
            String reason = (String) itemData.get("reason");

            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new RuntimeException("Sale not found: " + saleId));

            if (sale.getSaleDate().isBefore(LocalDateTime.now().minusDays(3))) {
                throw new IllegalArgumentException("Return policy expired (3 days). Sale Date: " + sale.getSaleDate());
            }

            SaleItem saleItem = sale.getItems().stream()
                    .filter(si -> si.getProductId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Item not found in sale"));

            double price = saleItem.getPrice();
            if (saleItem.getDiscount() != null && saleItem.getDiscount() > 0) {
                 price = price * (1 - saleItem.getDiscount() / 100.0);
            }
             
            double refundLine = price * quantity;

            DamageItem damage = new DamageItem();
            damage.setOriginalSaleId(saleId);
            damage.setProductId(productId);
            damage.setProductName(saleItem.getProductName());
            damage.setQuantity(quantity);
            damage.setReason(reason);
            damage.setRefundAmount(refundLine);
            damage.setReturnDate(LocalDateTime.now());
            
            if (user.getRole() == User.Role.OWNER) {
                damage.setReturnedBy("Owner");
            } else if (user.getRole() == User.Role.STORE_ADMIN) {
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

    public List<DamageItem> getPendingRequests(Long storeId) {
        if (storeId != null) {
            return damageItemRepository.findByStatusAndStoreId("PENDING", storeId);
        }
        return damageItemRepository.findByStatus("PENDING");
    }

    public List<DamageItem> getApprovedRequests(Long storeId) {
        if (storeId != null) {
            return damageItemRepository.findByStatusAndStoreId("APPROVED", storeId);
        }
        return damageItemRepository.findByStatus("APPROVED");
    }

    public List<DamageItem> getAllRequests(Long storeId) {
        if (storeId != null) {
            return damageItemRepository.findByStoreId(storeId);
        }
        return damageItemRepository.findAll();
    }

    public DamageItem approveRequest(User user, Long id) {
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
    
    public DamageItem rejectRequest(User user, Long id) {
        DamageItem item = damageItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        item.setStatus("REJECTED");
        item.setApprovedBy(user.getUsername());
        item.setApprovalDate(LocalDateTime.now());
        
        return damageItemRepository.save(item);
    }

    @Transactional
    public Voucher issueVoucher(List<Long> damageItemIds) {
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
        }

        // Delegate voucher creation to VoucherService
        Voucher saved = voucherService.issueVoucher(totalRefund, refSaleId);
        
        // Link Code
        for (Long id : damageItemIds) {
             DamageItem item = damageItemRepository.findById(id).get();
             item.setVoucherCode(saved.getCode());
             damageItemRepository.save(item);
        }
        
        return saved;
    }

    public List<DamageItem> getReturnsBySaleId(Long saleId) {
        return damageItemRepository.findByOriginalSaleId(saleId);
    }
}
