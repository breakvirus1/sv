package com.example.statisticservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "statistic_material_consumptions", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.statistic_material_consumptions SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticMaterialConsumption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statistic_order_item_id", nullable = false)
    private StatisticOrderItem statisticOrderItem;

    @Column(name = "order_material_id")
    private Long orderMaterialId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "material_name", length = 255, nullable = false)
    private String materialName;

    @Column(name = "material_unit", length = 20)
    private String materialUnit;

    @Column(name = "material_price", precision = 12, scale = 2)
    private BigDecimal materialPrice = BigDecimal.ZERO;

    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "width_m", precision = 10, scale = 4)
    private BigDecimal widthM;

    @Column(name = "height_m", precision = 10, scale = 4)
    private BigDecimal heightM;

    @Column(name = "net_quantity", precision = 12, scale = 2)
    private BigDecimal netQuantity = BigDecimal.ZERO;

    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "cost_priceplus", precision = 12, scale = 2)
    private BigDecimal costPriceplus = BigDecimal.ZERO;

    @Column(name = "eyelet_cost", precision = 12, scale = 2)
    private BigDecimal eyeletCost = BigDecimal.ZERO;

    @Column(name = "order_client_priceplus", precision = 10, scale = 2)
    private BigDecimal orderClientPriceplus = BigDecimal.ZERO;

    @Column(name = "consumption_with_priceplus", precision = 12, scale = 2)
    private BigDecimal consumptionWithPriceplus = BigDecimal.ZERO;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;
}
