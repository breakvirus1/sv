package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderOperationRequest {
    private Long operationId;
    /** Ширина в метрах (опционально) */
    private BigDecimal widthM;
    /** Высота в метрах (опционально) */
    private BigDecimal heightM;
}
