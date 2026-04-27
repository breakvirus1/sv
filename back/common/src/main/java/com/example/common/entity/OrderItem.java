package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Позиция заказа" — конкретное изделие в заказе.
 * Пример: "Баннер 2,7x1,38м".
 * Связана с заказом и содержит материалы, использованные для этой позиции.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem extends BaseEntity {

    /** Заказ, к которому относится позиция */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Наименование изделия */
    @Column(nullable = false, length = 255)
    private String name;

    /** Цена за единицу */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** Количество */
    @Column(name = "quantity")
    private Integer quantity = 1;

    /** Общая стоимость позиции (price * quantity) */
    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** Планируемая дата готовности этой позиции */
    @Column(name = "ready_date")
    private LocalDate readyDate;

    /** Материалы, использованные для этой позиции */
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderMaterial> materials = new ArrayList<>();
}
