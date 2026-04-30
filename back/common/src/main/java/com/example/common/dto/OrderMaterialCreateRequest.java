package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для создания позиции заказа в виде материала.
 * Используется при создании заказа для указания материала, его количества и срока готовности.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialCreateRequest {
    /** ID материала из справочника */
    private Long materialId;
    /** Количество в единицах измерения материала (например, метры, квадратные метры) */
    private BigDecimal quantity;
    /** Срок готовности позиции (опционально) */
    private LocalDate readyDate;
}
