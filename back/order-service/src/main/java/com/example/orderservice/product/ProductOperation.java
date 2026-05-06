package com.example.orderservice.product;

import com.example.orderservice.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Duration;

@Entity
@Table(name = "product_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductOperation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private String name;
    private BigDecimal pricePerUnit;
    private Duration normTime;
    private String unit = "шт";
    private Integer sortOrder;

    /** Формула расчёта количества (например "helper.eyeletCount(width, height, step, edgeDistance, quantity)"). Если NULL, используется quantity заказа */
    @Column(name = "quantity_formula", length = 500)
    private String quantityFormula;
}
