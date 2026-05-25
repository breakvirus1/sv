package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Material calculation info for CalculatedOrderResponse.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialCalculation {
    private Long id;
    private Long materialId;
    private String materialName;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private BigDecimal cost;
    private BigDecimal costPriceplus;
}