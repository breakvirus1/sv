package com.example.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Заказ" — основная сущность системы.
 * Содержит общую информацию о заказе: клиент, сумма, статус, стадия производства.
 * Связана с позициями (OrderItem), этапами (OrderStage), оплатами (Payment),
 * комментариями (OrderComment) и файлами (FileAttachment).
 *
 * Использует мягкое удаление (soft delete) через поле deleted.
 */
@SuppressWarnings("deprecation")
@Entity
@Table(name = "orders")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SQLDelete(sql = "UPDATE orders SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
public class Order extends BaseEntity {

    /** Уникальный номер заказа (например, "З-001", "2024/12/001") */
    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    /** Клиент, который разместил заказ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** Описание заказа (что нужно сделать) */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Общая сумма заказа (рассчитывается из позиций) */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /** Общая сумма оплат по заказу */
    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    /** Текущий долг (total_amount - paid_amount) */
    @Column(name = "debt_amount", precision = 12, scale = 2)
    private BigDecimal debtAmount = BigDecimal.ZERO;

    /** Текущий статус заказа */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private OrderStatus status = OrderStatus.WAITING;

    /** Текущая стадия производства */
    @Enumerated(EnumType.STRING)
    @Column(name = "production_stage", length = 50)
    private ProductionStage productionStage = ProductionStage.NOT_STARTED;

    /** Дата создания заказа */
    @Column(name = "order_date")
    private LocalDate orderDate;

    /** Планируемая дата сдачи заказа клиенту */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Менеджер, ответственный за заказ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    /** Дата и время запуска заказа в производство */
    @Column(name = "launched_at")
    private LocalDateTime launchedAt;

    /** Дата и время, когда заказ стал готовым */
    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    /** Дата и время, когда заказ был принят клиентом */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /** Дата и время окончательного закрытия заказа */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Флаг наличия документов (согласие, макет, акт и т.п.) */
    @Column(name = "has_documents")
    private Boolean hasDocuments = false;

    /** Позиции заказа (изделия) */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<OrderItem> items = new ArrayList<>();

    /** Этапы производства по цехам */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<OrderStage> stages = new ArrayList<>();

    /** Платежи по заказу */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments = new ArrayList<>();

    /** Комментарии и сообщения по заказу */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<OrderComment> comments = new ArrayList<>();

    /** Материалы, использованные в заказе (на уровне заказа) */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<OrderMaterial> materials = new ArrayList<>();
}
