package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO для создания позиции заказа (заказанного изделия).
 * Используется для добавления изделия в заказ на основе продукта-шаблона.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemCreateRequest {
    /** ID заказа */
    private Long orderId;

    /** Наименование изделия (если не указано, будет взято из продукта) */
    private String name;

    /** Ширина изделия (в метрах) */
    private BigDecimal width;

    /** Высота изделия (в метрах) */
    private BigDecimal height;

    /** Количество */
    private Integer quantity = 1;

    /** ID продукта-шаблона (может быть null для ручного ввода) */
    private Long productId;

    /** Дополнительные параметры (цвет, толщина, крепление и т.д.) */
    private Map<String, Object> params;

    /** Планируемая дата готовности */
    private LocalDate readyDate;
}