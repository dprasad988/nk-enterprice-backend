package com.hardwarepos.controller;

import com.hardwarepos.entity.DamageItem;
import com.hardwarepos.entity.User;
import com.hardwarepos.entity.Voucher;
import com.hardwarepos.repository.UserRepository;
import com.hardwarepos.service.ReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @Autowired
    private com.hardwarepos.service.SecurityService securityService;

    @PostMapping("/request")
    public List<DamageItem> createReturnRequest(@RequestBody List<Map<String, Object>> returnItems) {
        return returnService.createReturnRequest(securityService.getAuthenticatedUser(), returnItems);
    }

    @GetMapping("/pending")
    public List<DamageItem> getPendingRequests(@RequestParam(required = false) Long storeId) {
        return returnService.getPendingRequests(storeId);
    }

    @GetMapping("/approved")
    public List<DamageItem> getApprovedRequests(@RequestParam(required = false) Long storeId) {
        return returnService.getApprovedRequests(storeId);
    }

    @GetMapping("/all")
    public List<DamageItem> getAllRequests(@RequestParam(required = false) Long storeId) {
        return returnService.getAllRequests(storeId);
    }

    @PostMapping("/{id}/approve")
    public DamageItem approveRequest(@PathVariable Long id) {
        return returnService.approveRequest(securityService.getAuthenticatedUser(), id);
    }
    
    @PostMapping("/{id}/reject")
    public DamageItem rejectRequest(@PathVariable Long id) {
        return returnService.rejectRequest(securityService.getAuthenticatedUser(), id);
    }

    @PostMapping("/issue-voucher")
    public Voucher issueVoucher(@RequestBody List<Long> damageItemIds) {
        return returnService.issueVoucher(damageItemIds);
    }

    @GetMapping("/sale/{saleId}")
    public List<DamageItem> getReturnsBySaleId(@PathVariable Long saleId) {
        return returnService.getReturnsBySaleId(saleId);
    }
}
