package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO для оплаты заказа.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    /** ID оплаты */
    private Long id;
    /** Сумма оплаты */
    private BigDecimal amount;
    /** Дата оплаты */
    private LocalDate paymentDate;
    /** Вид оплаты: Безнал, Нал, Карта и т.д. */
    private String paymentType;
    /** Детали оплаты (примечание, номер счета и т.п.) */
    private String details;
    /** Флаг частичной оплаты */
    private Boolean isPartial;
}
