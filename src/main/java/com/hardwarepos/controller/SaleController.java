package com.hardwarepos.controller;

import com.hardwarepos.entity.Sale;
import com.hardwarepos.entity.SaleAuditLog;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.UserRepository;
import com.hardwarepos.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    @Autowired
    private SaleService saleService;

    @Autowired
    private com.hardwarepos.service.SecurityService securityService;

    @GetMapping("/{id}")
    public Sale getSaleById(@PathVariable Long id) {
        return saleService.getSaleById(id);
    }

    @GetMapping("/{id}/exchangeable")
    public Sale getExchangeableSale(@PathVariable Long id) {
        return saleService.getExchangeableSale(id);
    }

    @GetMapping
    public List<Sale> getAllSales(@RequestParam(required = false) Long storeId) {
        return saleService.getAllSales(securityService.getAuthenticatedUser(), storeId);
    }

    @PostMapping
    public Sale createSale(@RequestBody Sale sale) {
        return saleService.createSale(securityService.getAuthenticatedUser(), sale);
    }

    @PutMapping("/{id}")
    public Sale updateSale(@PathVariable Long id, @RequestBody Sale updatedSale) {
        return saleService.updateSale(securityService.getAuthenticatedUser(), id, updatedSale);
    }

    @GetMapping("/logs")
    public Page<SaleAuditLog> getSaleLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long storeId
    ) {
        return saleService.getSaleLogs(securityService.getAuthenticatedUser(), page, size, startDate, endDate, storeId);
    }
    @GetMapping("/{id}/versions")
    public List<com.hardwarepos.entity.SaleVersion> getSaleVersions(@PathVariable Long id) {
        return saleService.getSaleVersions(id);
    }
}
