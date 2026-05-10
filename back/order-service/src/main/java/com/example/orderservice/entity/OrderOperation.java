package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Сущность "Операция позиции заказа" — операция, примененная к позиции.
 */
@Entity
@Table(name = "order_item_operations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderOperation extends BaseEntity {

    /** Позиция заказа */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    /** ID операции из calculator-service */
    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    /** Название операции (для отображения) */
    @Column(name = "operation_name", nullable = false)
    private String operationName;

    /** Цена операции за единицу */
    @Column(name = "price_per_unit", precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    /** Рассчитанное количество */
    @Column(name = "calculated_quantity", precision = 12, scale = 4)
    private BigDecimal calculatedQuantity;

    /** Стоимость операции */
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;
}