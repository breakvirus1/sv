package com.example.orderservice.controller;

import com.example.orderservice.dto.ProductEstimateDTO;
import com.example.orderservice.product.dto.ProductDTO;
import com.example.orderservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> create(@RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductDTO> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/estimate")
    public ResponseEntity<ProductEstimateDTO> calculateEstimate(@PathVariable Long id) {
        ProductDTO product = productService.calculateEstimate(id);
        BigDecimal totalMaterials = BigDecimal.ZERO;
        BigDecimal totalOperations = BigDecimal.ZERO;
        if (product.getMaterials() != null) {
            for (var m : product.getMaterials()) {
                BigDecimal qty = m.getQuantity() != null ? m.getQuantity() : BigDecimal.ONE;
                BigDecimal waste = m.getWasteCoefficient() != null ? m.getWasteCoefficient() : BigDecimal.ONE;
                BigDecimal price = m.getPrice() != null ? m.getPrice() : BigDecimal.ZERO;
                totalMaterials = totalMaterials.add(qty.multiply(waste).multiply(price));
            }
        }
        if (product.getOperations() != null) {
            for (var o : product.getOperations()) {
                BigDecimal qty = o.getQuantity() != null ? o.getQuantity() : BigDecimal.ONE;
                BigDecimal coeff = o.getCoefficient() != null ? o.getCoefficient() : BigDecimal.ONE;
                BigDecimal price = o.getPricePerUnit() != null ? o.getPricePerUnit() : BigDecimal.ZERO;
                totalOperations = totalOperations.add(price.multiply(qty).multiply(coeff));
            }
        }
        BigDecimal grandTotal = totalMaterials.add(totalOperations);
        return ResponseEntity.ok(new ProductEstimateDTO(id, product.getMaterials(), product.getOperations(), totalMaterials, totalOperations, grandTotal));
    }
}
