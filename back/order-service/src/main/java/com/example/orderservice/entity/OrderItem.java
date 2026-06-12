package com.example.orderservice.entity;

import com.example.orderservice.entity.OrderOperation;
import com.example.orderservice.entity.OrderMaterial;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "order_items")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Cache(region = "OrderItem", usage = CacheConcurrencyStrategy.READ_WRITE)
public class OrderItem extends BaseEntity {

    /** Заказ, к которому относится позиция */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Наименование изделия */
    @Column(nullable = false, length = 255)
    private String name;

    /** Ширина изделия (в метрах) */
    @Column(name = "width", precision = 10, scale = 3)
    private BigDecimal width;

    /** Высота изделия (в метрах) */
    @Column(name = "height", precision = 10, scale = 3)
    private BigDecimal height;

    /** Цена за единицу (продажная) */
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

    /** Продукт-шаблон, на основе которого создана позиция */
    @Column(name = "product_id")
    private Long productId;

    /** Параметры изделия в формате JSON (цвет, толщина, тип крепления и т.д.) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String params;

    /** Материалы, переопределённые для этой позиции */
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderMaterial> materials = new HashSet<>();

    /** Операции/работы, переопределённые для этой позиции */
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderOperation> operations = new HashSet<>();
}
