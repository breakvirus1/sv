package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для материала, использованного в заказе.
 * Связывает заказ/позицию с материалом из справочника.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialDto {
    /** ID записи */
    private Long id;
    /** Материал (сокращенное представление) */
    private MaterialDto material;
    /** Количество материала */
    private BigDecimal quantity;
    /** Планируемая дата готовности */
    private LocalDate readyDate;
    /** Коэффициент отхода */
    private BigDecimal wasteCoefficient;
    /** Стоимость материала для этой позиции/заказа */
    private BigDecimal cost;
}