package com.example.orderservice.controller;

import com.example.common.dto.*;
import com.example.common.entity.Order;
import com.example.common.entity.OrderStatus;
import com.example.common.entity.ProductionStage;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller для управления заказами.
 * Предоставляет CRUD операции, а также управление статусами и этапами.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Service", description = "API для управления заказами")
public class OrderController {

    private final OrderService orderService;

    /**
     * Получить список заказов с фильтрами и пагинацией.
     * Доступно: ADMIN, MANAGER, PRODUCTION, ACCOUNTANT.
     */
    @Operation(summary = "Получить список заказов с фильтрами")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT')")
    public ResponseEntity<Page<OrderDto>> getAllOrders(
            @Parameter(description = "Статус заказа") @RequestParam(required = false) String status,
            @Parameter(description = "ID менеджера") @RequestParam(required = false) Long managerId,
            @Parameter(description = "Дата с") @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "Дата по") @RequestParam(required = false) LocalDate toDate,
            Pageable pageable) {

        Specification<Order> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), OrderStatus.valueOf(status)));
        }
        if (managerId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("manager").get("id"), managerId));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("orderDate"), toDate));
        }

        return ResponseEntity.ok(orderService.getAllOrders(spec, pageable));
    }

    /**
     * Получить детальную информацию о заказе.
     * Доступно: ADMIN, MANAGER, PRODUCTION, ACCOUNTANT.
     */
    @Operation(summary = "Получить детальную информацию о заказе")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT')")
    public ResponseEntity<OrderDto> getOrder(@Parameter(description = "ID заказа") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Создать новый заказ.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Создать новый заказ")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderCreateRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    /**
     * Обновить статус заказа.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Обновить статус заказа")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderDto> updateStatus(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    /**
     * Обновить стадию производства.
     * Доступно: ADMIN, PRODUCTION.
     */
    @Operation(summary = "Обновить стадию производства")
    @PutMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION')")
    public ResponseEntity<OrderDto> updateStage(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestParam String stage) {
        return ResponseEntity.ok(orderService.updateProductionStage(id, stage));
    }

    /**
     * Добавить оплату к заказу.
     * Доступно: ADMIN, ACCOUNTANT.
     */
    @Operation(summary = "Добавить оплату к заказу")
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ResponseEntity<Void> addPayment(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestBody PaymentDto payment) {
        orderService.addPayment(id, payment);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
