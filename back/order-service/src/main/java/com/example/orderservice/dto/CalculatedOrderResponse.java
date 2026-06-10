package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO для заказа с рассчитанными позициями с учетом priceplus.
 * Используется фронтендом для отображения расчетов в реальном времени.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculatedOrderResponse {
    private Long orderId;
    private BigDecimal priceplus;
    private BigDecimal totalWithoutPriceplus;
    private BigDecimal totalWithPriceplus;
    private List<MaterialCalculation> materials;
}