package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderEstimateDTO;
import com.example.orderservice.product.dto.ProductDTO;
import com.example.orderservice.service.ProductService;
import com.example.orderservice.service.EstimateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@CrossOrigin
public class ProductController {

    private final ProductService productService;
    private final EstimateService estimateService;

    @PostMapping
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

    @GetMapping("/{id}/estimate")
    public ResponseEntity<OrderEstimateDTO> calculateEstimate(@PathVariable Long id) {
        return ResponseEntity.ok(estimateService.calculateFromProduct(id));
    }
}
