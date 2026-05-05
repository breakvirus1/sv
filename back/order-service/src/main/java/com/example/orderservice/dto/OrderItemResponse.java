package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO для позиции заказа (читающее представление).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    /** ID позиции */
    private Long id;
    /** Наименование изделия */
    private String name;
    /** Ширина изделия (м) */
    private BigDecimal width;
    /** Высота изделия (м) */
    private BigDecimal height;
    /** Цена за единицу */
    private BigDecimal price;
    /** Количество */
    private Integer quantity;
    /** Общая стоимость (price * quantity) */
    private BigDecimal cost;
    /** Дата готовности позиции */
    private LocalDate readyDate;
    /** ID продукта-шаблона (если позиция создана из шаблона) */
    private Long productId;
    /** Продукт-шаблон (вложенный объект, опционально) */
    private Object product;
    /** Параметры изделия (JSON) */
    private String params;
}
