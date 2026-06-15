package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Позиция расчёта — связь между расчётом и операцией с рассчитанными значениями.
 */
@Entity
@Table(name = "calculator_calculation_operations")
@SQLDelete(sql = "UPDATE calculator_calculation_operations SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class CalculationOperation extends BaseEntity {

    /** Расчёт, к которому относится операция */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculation_id", nullable = false)
    private Calculation calculation;

    /** Операция из справочника */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operation_id", nullable = false)
    private Operation operation;

    /** Количество (м², п.м. или шт) */
    @Column(name = "quantity", precision = 12, scale = 4)
    private BigDecimal quantity;

    /** Цена за единицу на момент расчёта */
    @Column(name = "price_per_unit", precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    /** Сумма (quantity * pricePerUnit) */
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal;
}
