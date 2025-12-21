package com.hardwarepos.controller;

import com.hardwarepos.entity.Voucher;
import com.hardwarepos.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
@CrossOrigin(origins = "*")
public class VoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @PostMapping("/verify")
    public Voucher verifyVoucher(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid Voucher Code"));

        if (!"ACTIVE".equals(voucher.getStatus())) {
            throw new RuntimeException("Voucher is not active (Status: " + voucher.getStatus() + ")");
        }
        
        // Check Expiry
        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
             voucher.setStatus("EXPIRED");
             voucherRepository.save(voucher);
             throw new RuntimeException("Voucher Expired");
        }

        if (voucher.getCurrentBalance() <= 0) {
            throw new RuntimeException("Voucher has no balance");
        }

        return voucher;
    }

    @GetMapping("/sale/{saleId}")
    public java.util.List<Voucher> getVouchersForSale(@PathVariable Long saleId) {
        return voucherRepository.findByIssuedForReturnIdAndStatus(saleId, "ACTIVE");
    }
}
