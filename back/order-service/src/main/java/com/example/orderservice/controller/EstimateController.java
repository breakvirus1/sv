package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderEstimateDTO;
import com.example.orderservice.service.EstimateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class EstimateController {

    private final EstimateService estimateService;

    @GetMapping("/items/{orderItemId}/estimate")
    public ResponseEntity<OrderEstimateDTO> getEstimate(@PathVariable Long orderItemId) {
        return ResponseEntity.ok(estimateService.calculateEstimate(orderItemId));
    }

    @PostMapping("/items/{orderItemId}/estimate/from-product")
    public ResponseEntity<OrderEstimateDTO> createFromProduct(
            @PathVariable Long orderItemId,
            @RequestBody Long productId) {
        // Можно сразу сохранить, но по спецификации возвращаем расчёт из продукта
        return ResponseEntity.ok(estimateService.calculateFromProduct(productId));
    }

    @PostMapping("/items/{orderItemId}/estimate")
    public ResponseEntity<OrderEstimateDTO> saveEstimate(
            @PathVariable Long orderItemId,
            @RequestBody OrderEstimateDTO request) {
        return ResponseEntity.ok(estimateService.saveEstimate(orderItemId, request));
    }
}
