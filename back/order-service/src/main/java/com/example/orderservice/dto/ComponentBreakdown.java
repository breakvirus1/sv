package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Строка детализации расчёта (материал или операция).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComponentBreakdown {
    /** Наименование компонента */
    private String name;

    /** ID материала (если компонент - материал, иначе null) */
    private Long materialId;

    /** Количество (с учётом отходов) */
    private BigDecimal quantity;

    /** Цена за единицу */
    private BigDecimal unitPrice;

    /** Итоговая стоимость */
    private BigDecimal total;
}
