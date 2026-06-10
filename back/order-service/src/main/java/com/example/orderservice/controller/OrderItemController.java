package com.example.orderservice.controller;

import com.example.orderservice.dto.OrderItemCreateRequest;
import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Items", description = "API для управления позициями заказов")
public class OrderItemController {

    private final OrderService orderService;

    /**
     * Добавить новую позицию (изделие) в заказ.
     * Доступно: ADMIN, MANAGER.
     * Если указан productId, позиция рассчитывается динамически по формуле продукта.
     */
    @Operation(summary = "Добавить позицию в заказ")
    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderItemResponse> addOrderItem(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemCreateRequest request) {
        // Ensure path orderId matches body if provided
        request.setOrderId(orderId);
        return ResponseEntity.ok(orderService.addOrderItem(request));
    }
}