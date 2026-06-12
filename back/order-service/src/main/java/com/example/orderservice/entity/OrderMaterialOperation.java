package com.example.orderservice.entity;

import com.example.orderservice.entity.BaseEntity;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialOperation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "order_material_operations")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Cache(region = "OrderMaterialOperation", usage = CacheConcurrencyStrategy.READ_WRITE)
public class OrderMaterialOperation extends BaseEntity {

    /** Заказ (родительский) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Позиция заказа (материал), к которой относится операция */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_material_id", nullable = false)
    private OrderMaterial orderMaterial;

    /** Шаблон операции материала (опционально, может быть null если операция создана вручную) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_operation_id")
    private MaterialOperation template;

    /** Название операции (копируется из шаблона или вводится вручную) */
    @Column(nullable = false, length = 255)
    private String name;

    /** Тип операции */
    @Column(name = "operation_type", length = 30)
    private String operationType;

    /** Базовая цена за единицу */
    @Column(name = "base_price", precision = 12, scale = 2)
    private BigDecimal basePrice;

    /** Единица измерения */
    @Column(name = "unit", length = 20)
    private String unit = "шт";

    /** Коэффициент отходов */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    @ColumnDefault("1.0")
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    /** Количество (площадь, длина, шт) */
    @Column(name = "quantity", precision = 12, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;

    /** Фактическая стоимость операции (рассчитывается) */
    @Column(name = "cost", precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** Параметры операции в JSON (ширина, плотность и т.д.) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String parameters;

    /** Дополнительные материалы JSON: [{materialId, quantity, unit, price}] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String additionalMaterials;

    /** Признак активности (soft delete) */
    @Column(name = "active")
    @ColumnDefault("true")
    private Boolean active = true;
}
