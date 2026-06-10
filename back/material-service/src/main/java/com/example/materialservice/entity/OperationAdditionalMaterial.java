package com.example.materialservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

/**
 * Дополнительный материал, который используется в операции.
 * Пример: клей для кармана, люверсы, скотч.
 */
@Entity
@Table(name = "operation_additional_materials")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OperationAdditionalMaterial extends BaseEntity {

    /** Ссылка на операцию */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id", nullable = false)
    private MaterialOperation operation;

    /** Ссылка на материал из справочника */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Количество по умолчанию */
    @Column(name = "default_quantity", precision = 12, scale = 4)
    private BigDecimal defaultQuantity = BigDecimal.ONE;

    /** Единица измерения */
    @Column(name = "unit", length = 20)
    private String unit = "шт";

    /** Цена за единицу на момент создания шаблона (для истории) */
    @Column(name = "price_per_unit", precision = 12, scale = 2)
    private BigDecimal pricePerUnit;
}
