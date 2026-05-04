package com.example.orderservice.dto;

import com.example.materialservice.dto.MaterialResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO для материала, использованного в заказе.
 * Связывает заказ/позицию с материалом из справочника.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialResponse {
    /** ID записи */
    private Long id;
    /** Материал (сокращенное представление) */
    private MaterialResponse material;
    /** Количество материала */
    private BigDecimal quantity;
    /** Планируемая дата готовности */
    private LocalDate readyDate;
    /** Коэффициент отхода */
    private BigDecimal wasteCoefficient;
    /** Стоимость материала для этой позиции/заказа */
    private BigDecimal cost;
}
