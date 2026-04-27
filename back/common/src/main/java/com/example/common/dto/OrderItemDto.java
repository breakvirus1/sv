package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для позиции заказа (читающее представление).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {
    /** ID позиции */
    private Long id;
    /** Наименование изделия */
    private String name;
    /** Цена за единицу */
    private BigDecimal price;
    /** Количество */
    private Integer quantity;
    /** Общая стоимость (price * quantity) */
    private BigDecimal cost;
    /** Дата готовности позиции */
    private LocalDate readyDate;
}
