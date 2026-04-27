package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для позиции заказа (изделия).
 * Пример: "Баннер 2,7x1,38м", ценой 5000 ₽, количеством 2 шт.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemCreateRequest {
    /** Наименование изделия */
    private String name;
    /** Цена за единицу */
    private BigDecimal price;
    /** Количество */
    private Integer quantity;
    /** Планируемая дата готовности */
    private LocalDate readyDate;
}
