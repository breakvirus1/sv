package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Запрос на валидацию списка позиций заказа.
 * Требует указания ID заказа для проверки дубликатов.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateItemsRequest {
    /**
     * ID заказа, к которому относятся позиции.
     * Используется для проверки дубликатов среди уже существующих материалов в заказе.
     */
    private Long orderId;

    /**
     * Список позиций в таблице (текущее состояние фронтенда).
     */
    private List<OrderMaterialCreateRequest> items;
}
