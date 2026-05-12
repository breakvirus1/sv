package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO для операции позиции заказа (сводка).
 * Используется внутри OrderMaterialResponse для отображения операций.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderOperationSummary {
    /** ID операции из справочника */
    private Long operationId;
    /** Название операции */
    private String operationName;
    /** Цена за единицу */
    private BigDecimal pricePerUnit;
    /** Рассчитанное количество */
    private BigDecimal calculatedQuantity;
    /** Стоимость операции */
    private BigDecimal subtotal;
}
