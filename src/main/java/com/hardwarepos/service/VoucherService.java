package com.hardwarepos.service;

import com.hardwarepos.entity.Voucher;
import com.hardwarepos.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    public Voucher verifyVoucher(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid Voucher Code"));

        if (!"ACTIVE".equals(voucher.getStatus())) {
            throw new RuntimeException("Voucher is not active (Status: " + voucher.getStatus() + ")");
        }

        if (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            voucher.setStatus("EXPIRED");
            voucherRepository.save(voucher);
            throw new RuntimeException("Voucher Expired");
        }

        if (voucher.getCurrentBalance() <= 0) {
            throw new RuntimeException("Voucher has no balance");
        }

        return voucher;
    }

    public List<Voucher> getVouchersForSale(Long saleId) {
        return voucherRepository.findByIssuedForReturnIdAndStatus(saleId, "ACTIVE");
    }

    @Transactional
    public Voucher issueVoucher(Double amount, Long refSaleId) {
        Voucher voucher = new Voucher();
        voucher.setCode("V-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        voucher.setAmount(amount);
        voucher.setCurrentBalance(amount);
        voucher.setStatus("ACTIVE");
        voucher.setIssuedDate(LocalDateTime.now());
        voucher.setExpiryDate(LocalDateTime.now().plusMonths(6));
        voucher.setIssuedForReturnId(refSaleId);

        return voucherRepository.save(voucher);
    }
}
