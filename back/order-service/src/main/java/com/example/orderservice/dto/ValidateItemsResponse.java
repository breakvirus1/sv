package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ответ на валидацию списка позиций заказа.
 * Возвращает обогащённый список позиций (с подтянутыми defaults из БД),
 * флаг "есть дубликаты среди уже существующих материалов в заказе" и общую сумму.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidateItemsResponse {
    /**
     * true, если при валидации обнаружена ошибка (например дубликат)
     */
    private boolean error;

    /**
     * Текстовое сообщение об ошибке.
     */
    private String errorMessage;

    /**
     * Обогащённый список позиций с подтянутыми шириной/высотой из справочника.
     */
    private List<ItemPositionInfo> items;

    /**
     * Общая сумма заказа по существующим материалам.
     */
    private BigDecimal totalAmount;
}
