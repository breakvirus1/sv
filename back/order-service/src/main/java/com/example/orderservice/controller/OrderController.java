package com.example.orderservice.controller;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.ProductionStage;
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
     public ResponseEntity<Page<OrderResponse>> getAllOrders(
             @Parameter(description = "Статус заказа") @RequestParam(required = false) String status,
             @Parameter(description = "ID менеджера") @RequestParam(required = false) Long managerId,
             @Parameter(description = "ID клиента") @RequestParam(required = false) Long clientId,
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
         if (clientId != null) {
             spec = spec.and((root, query, cb) ->
                     cb.equal(root.get("client").get("id"), clientId));
         }
         if (fromDate != null) {
             spec = spec.and((root, query, cb) ->
                     cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate));
         }
         if (toDate != null) {
             spec = spec.and((root, query, cb) ->
                     cb.lessThanOrEqualTo(root.get("orderDate"), toDate));
         }

         Page<OrderResponse> page = orderService.getAllOrders(spec, pageable);
         return ResponseEntity.ok(page);
     }

    /**
     * Получить детальную информацию о заказе.
     * Доступно: ADMIN, MANAGER, PRODUCTION, ACCOUNTANT.
     */
    @Operation(summary = "Получить детальную информацию о заказе")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT')")
    public ResponseEntity<OrderResponse> getOrder(@Parameter(description = "ID заказа") @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Создать новый заказ.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Создать новый заказ")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderCreateRequest request) {
        return new ResponseEntity<>(orderService.createOrder(request), HttpStatus.CREATED);
    }

    /**
     * Обновить заказ.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Обновить заказ")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> updateOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestBody OrderUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    /**
     * Обновить статус заказа.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Обновить статус заказа")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> updateStatus(
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
    public ResponseEntity<OrderResponse> updateStage(
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
            @RequestBody PaymentRequest payment) {
        orderService.addPayment(id, payment);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

     /**
      * Добавить комментарий к заказу.
      * Доступно: ADMIN, MANAGER, PRODUCTION.
      */
     @Operation(summary = "Добавить комментарий к заказу")
     @PostMapping("/{id}/comments")
     @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION')")
     public ResponseEntity<CommentResponse> addComment(
             @Parameter(description = "ID заказа") @PathVariable Long id,
             @RequestBody CommentRequest request) {
         // Author will be extracted from authentication token in the service
         CommentResponse comment = orderService.addComment(id, request, null);
         return new ResponseEntity<>(comment, HttpStatus.CREATED);
     }
 }
