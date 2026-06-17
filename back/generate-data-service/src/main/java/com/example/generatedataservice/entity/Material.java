package com.example.generatedataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "materials", schema = "ordschema")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Material extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private MaterialType type;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "waste_coefficient", precision = 5, scale = 3)
    private BigDecimal wasteCoefficient = BigDecimal.ONE;

    @Column(name = "default_width_m", precision = 10, scale = 4)
    private BigDecimal defaultWidthM = BigDecimal.ZERO;

    @Column(name = "default_height_m", precision = 10, scale = 4)
    private BigDecimal defaultHeightM = BigDecimal.ZERO;
}
