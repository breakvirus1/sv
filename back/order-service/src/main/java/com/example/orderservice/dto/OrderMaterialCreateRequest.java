package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request для создания позиции заказа в виде материала.
 * Может включать операции, связанные с материалом.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterialCreateRequest {
    private Long id;
    private Long materialId;

    /** Количество в единицах измерения материала (например, метры, квадратные метры) */
    private BigDecimal quantity;

    /** Ширина изделия в метрах (для расчёта операций) */
    private BigDecimal width;

    /** Высота изделия в метрах (для расчёта операций) */
    private BigDecimal height;

    /** Количество изделий (для расчёта операций, например, люверсов) */
    private Integer itemCount;

    /** Срок готовности позиции (опционально) */
    private LocalDate readyDate;

    /** Список операций, привязанных к этому материалу */
    private List<OrderOperationCreateRequest> operations;
}
