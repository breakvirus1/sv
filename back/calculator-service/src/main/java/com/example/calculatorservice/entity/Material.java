package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Абстрактный базовый класс для материалов.
 * Материалы бывают двух типов: баннер (BANNER) и плёнка (PLENKA).
 * Цена указывается за 1 м².
 */
@Entity
@Table(name = "materials", schema = "ordschema")
@SQLDelete(sql = "UPDATE ordschema.materials SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class Material extends BaseEntity {

    /** Наименование материала */
    @Column(nullable = false, length = 255)
    private String name;

    /** Цена за 1 м² (отображение столбца 'price' в общей таблице материалов) */
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal pricePerSquareMeter;

    /** Коэффициент отхода (например, 1.10 = 10% отход) */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;
}
