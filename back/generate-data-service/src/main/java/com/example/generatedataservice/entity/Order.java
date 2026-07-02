package com.example.generatedataservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", schema = "svschema")
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "debt_amount", precision = 12, scale = 2)
    private BigDecimal debtAmount = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "margin_percent", precision = 8, scale = 2)
    private BigDecimal marginPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private ProductionStage status = ProductionStage.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "production_stage", length = 50)
    private ProductionStage productionStage = ProductionStage.DRAFT;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "priceplus", precision = 10, scale = 2)
    private BigDecimal priceplus;

    @Column(name = "total_with_priceplus", precision = 12, scale = 2)
    private BigDecimal totalWithPriceplus = BigDecimal.ZERO;

    @Column(name = "launched_at")
    private LocalDateTime launchedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "has_documents")
    private Boolean hasDocuments = false;
}
