package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO для операции материала в заказе.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialOperationResponse {
    private Long id;
    private String name;
    private BigDecimal pricePerUnit;
    private BigDecimal quantity;
    private BigDecimal cost;
    private BigDecimal wasteCoefficient;
    private String parameters;
    private String additionalMaterials;
}