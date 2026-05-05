package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Запрос на расчёт изделия по шаблону (Product).
 * Передаётся с фронтенда для динамического расчета.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculateRequest {
    /** ID шаблона продукта */
    private Long productId;

    /** Ширина изделия (в метрах) */
    private BigDecimal width;

    /** Высота изделия (в метрах) */
    private BigDecimal height;

    /** Количество */
    private Integer quantity = 1;

    /** Дополнительные параметры (цвет, толщина, тип крепления и т.д.) */
    private Map<String, Object> params;
}
