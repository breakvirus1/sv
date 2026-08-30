package com.example.statisticservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "statistic_order_items", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.statistic_order_items SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statistic_order_id", nullable = false)
    private StatisticOrder statisticOrder;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "ready_date")
    private LocalDate readyDate;

    @OneToMany(mappedBy = "statisticOrderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatisticMaterialConsumption> materials = new ArrayList<>();
}
