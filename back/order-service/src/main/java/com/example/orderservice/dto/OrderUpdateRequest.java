package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}
