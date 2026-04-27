package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для заказа (полное представление с вложенными объектами).
 * Используется как для списка (частично заполненный), так и для детального просмотра.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    /** ID заказа */
    private Long id;
    /** Номер заказа */
    private String orderNumber;
    /** Клиент */
    private ClientDto client;
    /** Описание */
    private String description;
    /** Общая сумма */
    private BigDecimal totalAmount;
    /** Оплачено */
    private BigDecimal paidAmount;
    /** Долг */
    private BigDecimal debtAmount;
    /** Статус заказа */
    private String status;
    /** Стадия производства */
    private String productionStage;
    /** Дата заказа */
    private LocalDate orderDate;
    /** Срок сдачи */
    private LocalDate dueDate;
    /** Менеджер */
    private EmployeeDto manager;
    /** Наличие документов */
    private Boolean hasDocuments;
    /** Дата создания */
    private LocalDateTime createdAt;
    /** Позиции заказа (заполняется только в детальном виде) */
    private List<OrderItemDto> items;
    /** Этапы производства (заполняется только в детальном виде) */
    private List<OrderStageDto> stages;
    /** Оплаты (заполняется только в детальном виде) */
    private List<PaymentDto> payments;
    /** Комментарии (заполняется только в детальном виде) */
    private List<CommentDto> comments;
    /** Материалы заказа (заполняется только в детальном виде) */
    private List<OrderMaterialDto> materials;
}
