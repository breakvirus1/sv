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
    /** Название операции */
    private String operationName;
    /** Цена за единицу */
    private BigDecimal pricePerUnit;
    /** Количество */
    private BigDecimal quantity;
    /** Ширина в метрах (опционально) */
    private BigDecimal widthM;
    /** Высота в метрах (опционально) */
    private BigDecimal heightM;
}
