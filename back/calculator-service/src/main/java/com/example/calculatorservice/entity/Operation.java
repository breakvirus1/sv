package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Сущность "Операция" — технологический шаг, применяемый к материалу.
 * Примеры: печать, резка, подворот, установка люверсов, сварка, ламинация.
 */
@Entity
@Table(name = "calculator_operations", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.calculator_operations SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class Operation extends BaseEntity {

    /** Наименование операции (например, "Печать 720 dpi", "Подворот") */
    @Column(nullable = false, length = 255)
    private String name;

    /** Цена за единицу измерения (за м², п.м. или шт) */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** Тип единицы измерения */
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", length = 20)
    private UnitType unit;

    /** Применимость операции: к баннеру, плёнке или обоим */
    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_to", length = 20)
    private ApplicableType applicableTo;

    /** Флаг, указывающий, является ли операция часто используемой */
    @Column(name = "is_default")
    private boolean isDefault = false;

    /** Параметры для операции "Подворот": ширина подворота в мм */
    @Column(name = "hem_width_mm")
    private Integer hemWidthMm;

    /** Параметры для операции "Подворот": количество подворотов на сторону */
    @Column(name = "hem_count")
    private Integer hemCount;
}
