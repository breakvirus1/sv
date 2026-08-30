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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "statistic_orders", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.statistic_orders SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticOrder extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", length = 50, nullable = false)
    private String orderNumber;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name", length = 255)
    private String clientName;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "priceplus", precision = 10, scale = 2)
    private BigDecimal priceplus = BigDecimal.ZERO;

    @Column(name = "total_with_priceplus", precision = 12, scale = 2)
    private BigDecimal totalWithPriceplus = BigDecimal.ZERO;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "production_stage", length = 50)
    private String productionStage;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "manager_name", length = 255)
    private String managerName;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @OneToMany(mappedBy = "statisticOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatisticOrderItem> items = new ArrayList<>();
}
