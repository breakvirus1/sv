package com.example.orderservice.order.entity;

import com.example.orderservice.entity.BaseEntity;
import com.example.orderservice.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Duration;

@Entity
@Table(name = "order_item_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemOperation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    private String name;
    private BigDecimal pricePerUnit;
    private Duration normTime;
    private BigDecimal quantity = BigDecimal.ONE;
    private BigDecimal cost;
}
