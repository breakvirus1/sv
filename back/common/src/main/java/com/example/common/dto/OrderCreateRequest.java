package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO для создания нового заказа.
 * Номер заказа генерируется автоматически на сервере.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateRequest {
    /** ID клиента */
    private Long clientId;
    /** Описание заказа */
    private String description;
    /** Дата заказа */
    private LocalDate orderDate;
    /** Срок сдачи */
    private LocalDate dueDate;
    /** ID менеджера */
    private Long managerId;
    /** Список позиций заказа (материалы) */
    private List<OrderMaterialCreateRequest> items;
}
