package com.example.orderservice.product;

import com.example.materialservice.entity.Material;
import com.example.orderservice.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductMaterial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Формула расчёта количества (например "width * height * 1.1"). Если NULL, используется поле quantity как константа */
    @Column(name = "quantity_formula", length = 500)
    private String quantityFormula;

    /** Базовое количество (используется если формула не задана) */
    private BigDecimal quantity;

    /** Коэффициент отхода */
    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    private Integer sortOrder;
}
