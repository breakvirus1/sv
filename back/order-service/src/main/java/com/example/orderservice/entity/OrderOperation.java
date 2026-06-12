package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_operations")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Cache(region = "OrderOperation", usage = CacheConcurrencyStrategy.READ_WRITE)
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

    /** Ширина в метрах (опционально, для операций с размерами) */
    @Column(name = "width_m", precision = 10, scale = 4)
    private BigDecimal widthM;

    /** Высота в метрах (опционально, для операций с размерами) */
    @Column(name = "height_m", precision = 10, scale = 4)
    private BigDecimal heightM;
}