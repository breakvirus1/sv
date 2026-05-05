package com.example.materialservice.entity;

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

    /** Артикул материала (уникальный) */
    @Column(name = "article", unique = true, length = 100)
    private String article;

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

    /** Поставщик материала */
    @Column(name = "supplier", length = 150)
    private String supplier;

    /** Текущий остаток на складе */
    @Column(name = "current_stock", precision = 12, scale = 4)
    private BigDecimal currentStock = BigDecimal.ZERO;

     /** Минимальный остаток (точка заказа) */
     @Column(name = "min_stock", precision = 12, scale = 4)
     private BigDecimal minStock;

     /** Список этапов операций, связанных с материалом (справочник) */
     @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = true)
     private java.util.List<MaterialOperation> operations = new java.util.ArrayList<>();
 }
