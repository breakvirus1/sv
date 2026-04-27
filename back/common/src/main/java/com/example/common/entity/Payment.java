package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сущность "Оплата" — запись об оплате заказа.
 * Может быть полной или частичной.
 * Пример: оплата "Безнал" на сумму 10 000 ₽ от 2024-12-01.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends BaseEntity {

    /** Заказ, к которому относится оплата */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Дата совершения оплаты */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** Сумма оплаты */
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    /** Вид оплаты: "Безнал", "Нал", "Карта", "Ин卡от" и т.д. */
    @Column(name = "payment_type", length = 50)
    private String paymentType;

    /** Детали оплаты (номер счета, примечание и т.п.) */
    @Column(length = 100)
    private String details;

    /** Флаг частичной оплаты (если true — сумма меньше общей) */
    @Column(name = "is_partial")
    private Boolean isPartial = false;
}
