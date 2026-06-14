package com.example.materialservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Шаблон операции над материалом.
 * Используется как этап в смете/заказе.
 */
@Entity
@Table(name = "material_operations", schema = "svtables")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MaterialOperation extends BaseEntity {

    /** Ссылка на материал, к которому привязана операция */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Название операции ("Печать на баннере", "Люверсы") */
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Тип операции (PRINT, LAMINATION, EYELETS, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 30, nullable = false)
    private OperationType operationType;

    /** Формула расчёта количества (например "helper.eyeletCount(step, edgeDistance, quantity)"). Если NULL, используется quantity из заказа */
    @Column(name = "quantity_formula", length = 500)
    private String quantityFormula;

    /** Базовая цена операции (за м², за пог.м, за шт.) */
    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    /** Единица измерения операции */
    @Column(name = "unit", length = 20)
    private String unit = "шт";

    /** Коэффициент отходов (1.1, 1.15 и т.д.) */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    @ColumnDefault("1.0")
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    /** Требует ли операция ввода размеров (ширина/высота) */
    @Column(name = "requires_dimensions")
    @ColumnDefault("false")
    private Boolean requiresDimensions = false;

    /** Можно ли добавлять дополнительные материалы к операции */
    @Column(name = "allows_additional_materials")
    @ColumnDefault("false")
    private Boolean allowsAdditionalMaterials = false;

    /** Динамические параметры операции (ширина, плотность и т.д.) */
    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<OperationParameter> parameters = new ArrayList<>();

    /** Дополнительные материалы, используемые в операции */
    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationAdditionalMaterial> additionalMaterials = new ArrayList<>();

    /** Порядок сортировки */
    @Column(name = "sort_order")
    @ColumnDefault("0")
    private Integer sortOrder = 0;

    /** Признак активности */
    @Column(name = "active")
    @ColumnDefault("true")
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null) setCreatedAt(LocalDateTime.now());
        if (getUpdatedAt() == null) setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }
}
