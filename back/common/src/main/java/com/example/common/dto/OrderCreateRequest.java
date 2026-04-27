package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для создания нового заказа.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateRequest {
    private String orderNumber;
    private Long clientId;
    private String description;
    private LocalDate orderDate;
    private LocalDate dueDate;
    private Long managerId;
    private java.util.List<OrderItemCreateRequest> items;
}
