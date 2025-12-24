package com.hardwarepos.controller;

import com.hardwarepos.entity.Voucher;
import com.hardwarepos.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @PostMapping("/verify")
    public Voucher verifyVoucher(@RequestBody Map<String, String> payload) {
        return voucherService.verifyVoucher(payload.get("code"));
    }

    @GetMapping("/sale/{saleId}")
    public List<Voucher> getVouchersForSale(@PathVariable Long saleId) {
        return voucherService.getVouchersForSale(saleId);
    }
}
