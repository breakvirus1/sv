package com.example.orderservice.dto;

import com.example.materialservice.dto.MaterialResponse;
import com.example.orderservice.dto.OrderOperationSummary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialResponse {
    private Long id;
    private MaterialResponse material;
    private BigDecimal quantity;
    /** Ширина изделия в метрах */
    private BigDecimal widthM;
    /** Высота изделия в метрах */
    private BigDecimal heightM;
    private LocalDate readyDate;
    private BigDecimal wasteCoefficient;
    private BigDecimal cost;
    /** Стоимость с учетом priceplus */
    private BigDecimal costPriceplus;
    /** Стоимость люверсов (eyelet hardware) */
    private BigDecimal eyeletCost;
    private List<OrderOperationSummary> operations;
    /** ID позиции заказа (OrderItem) */
    private Long orderItemId;
}
