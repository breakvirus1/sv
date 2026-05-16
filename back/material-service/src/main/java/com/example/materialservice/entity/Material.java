package com.example.materialservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Сущность "Материал" — номенклатура материалов и операций.
 * Используется для расчета себестоимости заказов.
 * Поддерживает мягкое удаление через @SQLDelete.
 */
@Entity
@Table(name = "materials", schema = "ordschema")
@SQLDelete(sql = "UPDATE ordschema.materials SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Material extends BaseEntity {

    /** Наименование материала или операции */
    @Column(nullable = false, length = 255)
    private String name;

    /** Тип записи — материал или операция */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private MaterialType type;

    /** Единица измерения: "м2", "шт", "п.м." и т.д. */
    @Column(name = "unit", length = 20)
    private String unit;

    /** Цена за единицу измерения */
    @Column(precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** Коэффициент отхода (например, 1.2 = 20% отход) */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    /** Ширина по умолчанию в метрах */
    @Column(name = "default_width_m", precision = 10, scale = 4)
    private BigDecimal defaultWidthM = BigDecimal.ZERO;

    /** Высота по умолчанию в метрах */
    @Column(name = "default_height_m", precision = 10, scale = 4)
    private BigDecimal defaultHeightM = BigDecimal.ZERO;
}
