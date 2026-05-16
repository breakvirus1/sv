package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderOperationSummary {
    private Long operationId;
    private String operationName;
    private BigDecimal pricePerUnit;
    private BigDecimal calculatedQuantity;
    private BigDecimal subtotal;
    /** Ширина в метрах (опционально) */
    private BigDecimal widthM;
    /** Высота в метрах (опционально) */
    private BigDecimal heightM;
}
