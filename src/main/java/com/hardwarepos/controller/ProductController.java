package com.hardwarepos.controller;

import com.hardwarepos.dto.ProductDTO;
import com.hardwarepos.entity.ProductAuditLog;
import com.hardwarepos.entity.User;
import com.hardwarepos.repository.UserRepository;
import com.hardwarepos.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private com.hardwarepos.service.SecurityService securityService;

    @GetMapping
    public List<ProductDTO> getAllProducts(@RequestParam(required = false) Long storeId) {
        return productService.getAllProducts(securityService.getAuthenticatedUser(), storeId);
    }

    @GetMapping("/paged")
    public Page<ProductDTO> getProductsPaged(
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search
    ) {
        return productService.getProductsPaged(securityService.getAuthenticatedUser(), storeId, page, size, search);
    }

    @GetMapping("/search")
    public List<ProductDTO> searchProducts(@RequestParam String query) {
        return productService.searchProducts(securityService.getAuthenticatedUser(), query);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('OWNER')")
    public ProductDTO addProduct(@RequestBody ProductDTO payload) {
        return productService.addProduct(securityService.getAuthenticatedUser(), payload);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('OWNER')")
    public ProductDTO updateProduct(@PathVariable Long id, @RequestBody ProductDTO payload) {
        return productService.updateProduct(securityService.getAuthenticatedUser(), id, payload);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('OWNER')")
    public void deleteProduct(@PathVariable Long id, @RequestParam(required = false) Long storeId) {
        productService.deleteProduct(securityService.getAuthenticatedUser(), id, storeId);
    }

    @GetMapping("/logs")
    public Page<ProductAuditLog> getProductLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long storeId
    ) {
        return productService.getProductLogs(securityService.getAuthenticatedUser(), page, size, startDate, endDate, storeId);
    }
}
