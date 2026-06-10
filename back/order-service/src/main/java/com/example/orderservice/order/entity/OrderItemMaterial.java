package com.example.orderservice.order.entity;

import com.example.materialservice.entity.Material;
import com.example.orderservice.entity.BaseEntity;
import com.example.orderservice.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    private BigDecimal quantity;
    private BigDecimal wasteCoefficient = BigDecimal.ONE;
    private BigDecimal cost;
}
