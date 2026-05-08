package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO для материала в заказе (позиция заказа).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialResponse {
    private Long id;
    private Long materialId;
    private String materialName;
    private BigDecimal quantity;
    private BigDecimal wasteCoefficient;
    private BigDecimal cost;
    private BigDecimal pricePerUnit;
    private String unit;
    private LocalDate readyDate;
    private List<OrderMaterialOperationResponse> operations;
}
