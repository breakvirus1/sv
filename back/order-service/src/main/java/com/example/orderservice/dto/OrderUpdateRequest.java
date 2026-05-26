package com.example.orderservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO для обновления заказа.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderUpdateRequest {
    /** Описание заказа */
    private String description;
    /** Дата заказа */
    private LocalDate orderDate;
    /** Срок сдачи */
    private LocalDate dueDate;
    /** ID менеджера */
    private Long managerId;
    /** Процент добавки клиента (priceplus) */
    private BigDecimal priceplus;
    /** Позиции заказа (материалы, ширина и высота в метрах) */
    private List<OrderMaterialCreateRequest> items;
    /** Итоговая сумма заказа (с учетом наценки priceplus) */
    private BigDecimal totalAmount;

    /** Расчёт суммы (с priceplus), выполненный фронтендом через calculation service. Для валидации на бэкенде с минимальной погрешностью. */
    private BigDecimal clientTotalWithPriceplus;
}
