package com.example.orderservice.entity;

import com.example.materialservice.entity.Material;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Материал в заказе" — связь заказа/позиции с материалом.
 * Хранит информацию об использовании материала для конкретного заказа.
 * Пример: для баннера 2,7x1,38м использован материал "Баннер лит. 450 гр/м2"
 * в количестве 5 м2 с коэффициентом отхода 1.2.
 */
@Entity
@Table(name = "order_materials")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderMaterial extends BaseEntity {

    /** Заказ (если материал назначен на весь заказ, а не на позицию) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /** Позиция заказа, к которой привязан материал (может быть null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    /** Материал из справочника */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Количество материала в единицах измерения */
    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    /** Планируемая дата готовности этой позиции (материала) */
    @Column(name = "ready_date")
    private LocalDate readyDate;

    /** Коэффициент отхода (применяется к базовой цене материала) */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    /** Фактическая стоимость материала для этого заказа (quantity * price * waste) */
    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** Операции, выполненные над этим материалом в заказе */
    @OneToMany(mappedBy = "orderMaterial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderMaterialOperation> operations = new ArrayList<>();
}

