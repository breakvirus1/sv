package com.example.materialservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;

/**
 * Сущность "Материал" — номенклатура материалов и операций.
 * Используется для расчета себестоимости заказов.
 * Поддерживает мягкое удаление через @SQLDelete.
 */
@Entity
@Table(name = "materials", schema = "ordschema")
@SQLDelete(sql = "UPDATE {table} SET deleted = true WHERE id=?")
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
