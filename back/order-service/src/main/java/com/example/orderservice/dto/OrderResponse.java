package com.example.orderservice.dto;

import com.example.clientservice.dto.ClientResponse;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.materialservice.dto.MaterialResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO для заказа (полное представление с вложенными объектами).
 * Используется как для списка (частично заполненный), так и для детального просмотра.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    /** ID заказа */
    private Long id;
    /** Номер заказа */
    private String orderNumber;
    /** Клиент */
    private ClientResponse client;
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
    private EmployeeResponse manager;
    /** Процент добавки клиента (priceplus) */
    private BigDecimal priceplus;
    /** Общая сумма заказа с учетом priceplus */
    private BigDecimal totalWithPriceplus;
    /** Наличие документов */
    private Boolean hasDocuments;
    /** Дата создания */
    private LocalDateTime createdAt;
    /** Дата последнего изменения */
    private LocalDateTime updatedAt;
    /** Позиции заказа (заполняется только в детальном виде) */
    private List<OrderItemResponse> items;
    /** Этапы производства (заполняется только в детальном виде) */
    private List<OrderStageResponse> stages;
    /** Оплаты (заполняется только в детальном виде) */
    private List<PaymentResponse> payments;
    /** Комментарии (заполняется только в детальном виде) */
    private List<CommentResponse> comments;
    /** Материалы заказа (заполняется только в детальном виде) */
    private List<OrderMaterialResponse> materials;
    /** ID цеха, к которому относится заказ */
    private Long workshopId;
}
