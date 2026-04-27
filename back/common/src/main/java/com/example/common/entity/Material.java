package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Сущность "Материал" — номенклатура материалов и операций.
 * Примеры: "Баннер лит. 450 гр/м2", "Проварка периметра", "Установка люверса".
 * Используется для расчета себестоимости заказов.
 */
@Entity
@Table(name = "materials")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Material extends BaseEntity {

    /** Наименование материала или операции */
    @Column(nullable = false, length = 255)
    private String name;

    /** Единица измерения: "м2", "шт", "п.м." и т.д. */
    @Column(name = "unit", length = 20)
    private String unit;

    /** Цена за единицу измерения */
    @Column(precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** Коэффициент отхода (например, 1.2 = 20% отход) */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;
}
