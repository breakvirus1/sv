package com.example.orderservice.controller;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.ProductionStage;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
     @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
      public ResponseEntity<Page<OrderResponse>> getAllOrders(
              @Parameter(description = "Статус заказа") @RequestParam(required = false) String status,
              @Parameter(description = "ID менеджера") @RequestParam(required = false) Long managerId,
              @Parameter(description = "ID клиента") @RequestParam(required = false) Long clientId,
              @Parameter(description = "Дата с") @RequestParam(required = false) LocalDate fromDate,
              @Parameter(description = "Дата по") @RequestParam(required = false) LocalDate toDate,
              @RequestParam(required = false, defaultValue = "50") Integer size,
              Pageable pageable,
              @AuthenticationPrincipal Jwt jwt) {

          Specification<Order> spec = Specification.where(null);

          if (status != null) {
              spec = spec.and((root, query, cb) ->
                       cb.equal(root.get("status"), ProductionStage.valueOf(status)));
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

           Pageable explicitPageable = PageRequest.of(pageable.getPageNumber(), size, pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt"));
           Page<OrderResponse> page = orderService.getAllOrders(spec, explicitPageable, scopedCompanyId(jwt));
           return ResponseEntity.ok(page);
        }

       @Operation(summary = "Получить опции для фильтров")
       @GetMapping("/filter-options")
       @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
       public ResponseEntity<java.util.Map<String, java.util.List<java.util.Map<String, Object>>>> getFilterOptions(
               @AuthenticationPrincipal Jwt jwt) {
           return ResponseEntity.ok(orderService.getFilterOptions(scopedCompanyId(jwt)));
       }

    /**
     * Получить детальную информацию о заказе.
     * Доступно: ADMIN, MANAGER, PRODUCTION, ACCOUNTANT.
     */
    @Operation(summary = "Получить детальную информацию о заказе")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<OrderResponse> getOrder(@Parameter(description = "ID заказа") @PathVariable Long id,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.getOrderById(id, scopedCompanyId(jwt)));
    }

    /**
     * Получить заказ по номеру заказа (orderNumber).
     * Формат номера: YYYYMMDDHHmmss (14 цифр).
     * Доступно: ADMIN, MANAGER, PRODUCTION, ACCOUNTANT.
     */
    @Operation(summary = "Получить заказ по номеру")
    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            @Parameter(description = "Номер заказа") @PathVariable String orderNumber,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.getOrderByOrderNumber(orderNumber, scopedCompanyId(jwt)));
    }

    /**
     * Создать новый заказ.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Создать новый заказ")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return new ResponseEntity<>(orderService.createOrder(request, scopedCompanyId(jwt)), HttpStatus.CREATED);
    }

    /**
     * Обновить заказ.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Обновить заказ")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<OrderResponse> updateOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.updateOrder(id, request, scopedCompanyId(jwt)));
    }

    /**
     * Обновить статус заказа.
     * Доступно: ADMIN, MANAGER.
     */
    @Operation(summary = "Обновить статус заказа")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<OrderResponse> updateStatus(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.updateStatus(id, status, scopedCompanyId(jwt)));
    }

    /**
     * Закрыть заказ.
     * Доступно только для ADMIN.
     */
    @Operation(summary = "Закрыть заказ")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'GOD')")
    public ResponseEntity<OrderResponse> closeOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.updateStatus(id, ProductionStage.CLOSED.name(), scopedCompanyId(jwt)));
    }

    @Operation(summary = "Отклонить заказ")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('PRODUCTION')")
    public ResponseEntity<OrderResponse> rejectOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestBody java.util.Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt) {
        String reason = request.getOrDefault("rejectionReason", "");
        return ResponseEntity.ok(orderService.rejectOrder(id, reason, scopedCompanyId(jwt)));
    }

    @Operation(summary = "Удалить заказ (мягкое удаление)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        orderService.deleteOrder(id, scopedCompanyId(jwt));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Обновить флаг готовности позиции заказа")
    @PutMapping("/{id}/items/{itemId}/ready")
    @PreAuthorize("hasRole('PRODUCTION')")
    public ResponseEntity<OrderItemResponse> updateOrderItemReady(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @Parameter(description = "ID позиции") @PathVariable Long itemId,
            @RequestBody java.util.Map<String, Boolean> request,
            @AuthenticationPrincipal Jwt jwt) {
        Boolean ready = request.get("ready");
        return ResponseEntity.ok(orderService.updateOrderItemReady(id, itemId, ready, scopedCompanyId(jwt)));
    }
    @Operation(summary = "Обновить стадию производства")
    @PutMapping("/{id}/stage")
    @PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION')")
    public ResponseEntity<OrderResponse> updateStage(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestParam String stage,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.updateProductionStage(id, stage, scopedCompanyId(jwt)));
    }

    /**
     * Добавить оплату к заказу.
     * Доступно: ADMIN, ACCOUNTANT.
     */
     @Operation(summary = "Получить список оплат по заказу")
     @GetMapping("/{id}/payments")
     @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
     public ResponseEntity<List<PaymentResponse>> getPayments(
             @Parameter(description = "ID заказа") @PathVariable Long id,
             @AuthenticationPrincipal Jwt jwt) {
         List<PaymentResponse> response = orderService.getPayments(id, scopedCompanyId(jwt));
         return ResponseEntity.ok(response);
     }

    @Operation(summary = "Добавить оплату к заказу")
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<PaymentResponse> addPayment(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestBody PaymentRequest payment,
            @AuthenticationPrincipal Jwt jwt) {
        PaymentResponse response = orderService.addPayment(id, payment, scopedCompanyId(jwt));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Добавить комментарий к заказу.
     * Доступно: ADMIN, MANAGER, PRODUCTION.
     */
    @Operation(summary = "Добавить комментарий к заказу")
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'GOD')")
    public ResponseEntity<CommentResponse> addComment(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @RequestBody CommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        // Author will be extracted from authentication token in the service
        CommentResponse comment = orderService.addComment(id, request, null, scopedCompanyId(jwt));
        return new ResponseEntity<>(comment, HttpStatus.CREATED);
    }


    // ===================== POSITION HELPERS (frontend EditOrder.jsx) =====================

    /**
     * Получить информацию о существующей позиции заказа (материал в заказе).
     * Подтягивает ширину и высоту: если в заказе не заданы, берёт defaultWidthMm/defaultHeightMm из справочника.
     *
     * Пример: GET /api/v1/orders/1/positions/7/default
     */
    @Operation(summary = "Получить информацию о существующей позиции заказа")
    @GetMapping("/{id}/positions/{orderMaterialId}/default")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'GOD')")
    public ResponseEntity<ItemPositionInfo> getItemPositionInfo(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @Parameter(description = "ID записи материала в заказе") @PathVariable Long orderMaterialId,
            @AuthenticationPrincipal Jwt jwt) {
        ItemPositionInfo info = orderService.getItemPositionInfo(id, orderMaterialId, scopedCompanyId(jwt));
        return ResponseEntity.ok(info);
    }

    /**
     * Рассчитать общую стоимость отрытого редактором заказа по существующим материалам.
     * Используется для отображения суммы пока заказ не сохранён.
     *
     * Пример: GET /api/v1/orders/1/total-open
     */
    @Operation(summary = "Получить открытую сумму отредактированного заказа")
    @GetMapping("/{id}/total-open")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<java.math.BigDecimal> getTotalOpen(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        java.math.BigDecimal total = orderService.calculateOpenOrderTotal(id, scopedCompanyId(jwt));
        return ResponseEntity.ok(total);
    }

    /**
     * Получить статистику заработка менеджера.
     * Включает заработок с заказов READY и IN_PROGRESS.
     */
    @Operation(summary = "Получить статистику заработка менеджера")
    @GetMapping("/manager-earnings/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'GOD')")
    public ResponseEntity<ManagerEarningsResponse> getManagerEarnings(
            @Parameter(description = "ID менеджера") @PathVariable Long managerId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(orderService.getManagerEarnings(managerId, scopedCompanyId(jwt)));
    }
    @Operation(summary = "Получить позиции заказа с расчетами priceplus")
    @GetMapping("/{id}/calculated")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PRODUCTION', 'ACCOUNTANT', 'GOD')")
    public ResponseEntity<CalculatedOrderResponse> getCalculatedOrder(
            @Parameter(description = "ID заказа") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        CalculatedOrderResponse response = orderService.getCalculatedOrder(id, scopedCompanyId(jwt));
        return ResponseEntity.ok(response);
    }

    private Long scopedCompanyId(Jwt jwt) {
        if (jwt == null) return null;
        if (isGod(jwt)) return extractCompanyId(jwt);
        return requireCompanyId(jwt);
    }

    private Long requireCompanyId(Jwt jwt) {
        Long companyId = extractCompanyId(jwt);
        if (companyId == null && jwt != null) {
            throw new org.springframework.security.access.AccessDeniedException("company_id claim is required");
        }
        return companyId;
    }

    private boolean isGod(Jwt jwt) {
        if (jwt == null) return false;
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> access) {
            Object roles = access.get("roles");
            if (roles instanceof java.util.List<?> list) {
                return list.stream().anyMatch(r -> "GOD".equals(String.valueOf(r)) || "ROLE_GOD".equals(String.valueOf(r)));
            }
        }
        return false;
    }

    private Long extractCompanyId(Jwt jwt) {
        if (jwt == null) return null;
        try {
            Object claim = jwt.getClaim("company_id");
            if (claim instanceof Number) {
                return ((Number) claim).longValue();
            }
            if (claim instanceof String) {
                return Long.parseLong((String) claim);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
