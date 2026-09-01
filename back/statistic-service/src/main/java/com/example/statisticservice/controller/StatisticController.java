package com.example.statisticservice.controller;

import com.example.statisticservice.dto.MaterialExpenseRow;
import com.example.statisticservice.service.OrderSyncService;
import com.example.statisticservice.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "API для статистики")
public class StatisticController {

    private final StatisticsService statisticsService;
    private final OrderSyncService orderSyncService;

    @Operation(summary = "Получить статистику расхода материалов за период")
    @GetMapping("/material-expense")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<List<MaterialExpenseRow>> getMaterialExpenseStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(statisticsService.getMaterialExpenseStatistics(fromDate, toDate));
    }

    @Operation(summary = "Синхронизировать заказ для статистики")
    @PostMapping("/sync-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> syncOrder(@RequestBody Map<String, Object> orderData) {
        orderSyncService.syncOrder(orderData);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить заказ из статистики")
    @DeleteMapping("/sync-order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteOrderFromStatistics(@PathVariable Long orderId) {
        orderSyncService.deleteByOrderId(orderId);
        return ResponseEntity.ok().build();
    }
}
