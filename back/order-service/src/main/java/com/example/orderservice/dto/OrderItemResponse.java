package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    /** Цена за единицу */
    private BigDecimal price;
    /** Количество */
    private Integer quantity;
    /** Общая стоимость (price * quantity) */
    private BigDecimal cost;
    /** Дата готовности позиции */
    private LocalDate readyDate;
    /** Операции, применённые к этой позиции */
    private List<OrderOperationSummary> operations;

    /** URL прикреплённого файла */
    private String fileUrl;

    /** ID прикреплённого файла */
    private Long fileId;
}
