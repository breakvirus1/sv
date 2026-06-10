package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Результат расчёта себестоимости и рекомендованной цены.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculationResult {
    /** Себестоимость материалов */
    private BigDecimal materialCost;

    /** Себестоимость операций */
    private BigDecimal operationCost;

    /** Общая себестоимость */
    private BigDecimal totalCost;

    /** Рекомендуемая цена продажи */
    private BigDecimal sellingPriceRecommended;

    /** Маржа в процентах */
    private BigDecimal marginPercent;

    /** Детализация по компонентам */
    private List<ComponentBreakdown> breakdown;
}
