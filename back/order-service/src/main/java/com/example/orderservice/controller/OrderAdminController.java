package com.example.orderservice.controller;

import com.example.common.entity.Order;
import com.example.orderservice.service.OrderAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Order Admin", description = "Админ API для управления заказами")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    @Operation(summary = "Сгенерировать 20 тестовых заказов")
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Order>> generateTestOrders() {
        List<Order> orders = orderAdminService.generateTestOrders(20);
        return ResponseEntity.ok(orders);
    }
}
