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

    private BigDecimal quantity;
    private BigDecimal wasteCoefficient = BigDecimal.ONE;
    private Integer sortOrder;
}
